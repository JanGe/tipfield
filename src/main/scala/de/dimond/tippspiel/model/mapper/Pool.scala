package de.dimond.tippspiel.model.mapper

import de.dimond.tippspiel.model._

import net.liftweb.mapper._
import net.liftweb.common._

import org.scala_tools.time.Imports._
import java.util.Date

object DbPool extends DbPool with LongKeyedMetaMapper[DbPool] with MetaPool {
  val secureRandom = new java.security.SecureRandom()
  private def randomInvitationId = {
    import java.math.BigInteger
    new BigInteger(150, secureRandom).toString(Character.MAX_RADIX).take(16)
  }

  override def newPool(name: String, description: String, allowMemberInvite: Boolean, admin: User): Box[Pool] = {
    val pool = DbPool.create
    pool._name(name)
    pool._description(description)
    pool._allowMemberInvite(allowMemberInvite)
    pool._adminId(admin.id)
    if (pool.save) {
      if (pool.inviteUser(admin.fbId, None) && pool.addUser(admin)) {
        Full(pool)
      } else {
        pool.delete_!
        Failure("Failed to add admin")
      }
    } else {
      Failure("Database Error")
    }
  }
  override def allForUser(user: User): Set[Pool] = {
    val poolIds = DbPoolMembership.findAll(By(DbPoolMembership.userId, user.id),
                                           By(DbPoolMembership.hasLeft, false)).map(_.pool.is)
    poolIds.map(forId(_)).flatten.toSet
  }
  override def forId(poolId: Long) = find(By(_id, poolId))
  override def findInvitationById(invitationId: String) = {
    DbInvitationLink.find(By(DbInvitationLink._invitationId, invitationId))
  }

  override def invitationsForUser(user: User, withIgnored: Boolean, withActive: Boolean): Set[Pool] = {
    val invitations = DbPoolInvites.findAll(By(DbPoolInvites.fbId, user.fbId))
    val filtered = invitations.filter(withIgnored || !_.ignored.is)
    if (withActive) {
      filtered.map(i => forId(i.pool.is)).flatten.toSet
    } else {
      val poolIds = DbPoolMembership.findAll(By(DbPoolMembership.userId, user.id),
                                             By(DbPoolMembership.hasLeft, false)).map(_.pool.is)
      filtered.filter(i => !poolIds.contains(i.pool.is)).map(i => forId(i.pool.is)).flatten.toSet
    }
  }
}

class DbPool extends Pool with LongKeyedMapper[DbPool] with Logger {
  def getSingleton = DbPool

  protected object _id extends MappedLongIndex(this)
  override def primaryKeyField = _id

  protected object _name extends MappedString(this, 64)
  protected object _description extends MappedString(this, 1024)
  protected object _adminId extends MappedLong(this)
  protected object _allowMemberInvite extends MappedBoolean(this)

  def id = _id.is
  def name = _name.is
  def description = _description.is
  def adminId = _adminId.is

  override def invitationLinkForUser(user: User) = {
    if ((_allowMemberInvite.is && users.contains(user.id)) || (user.id == adminId)) {
      DbInvitationLink.find(By(DbInvitationLink._userId, user.id), By(DbInvitationLink._pool, this)) match {
        case f: Full[_] => f
        case f: Failure => f
        case Empty => {
          val link = DbInvitationLink.create
          link._userId(user.id)
          link._pool(this)
          link._invitationId(DbPool.randomInvitationId)
          if (link.save()) {
            Full(link)
          } else {
            Failure("Failed to save invitation link!")
          }
        }
      }
    } else {
      Empty
    }
  }

  def removeUser(user: User): Boolean = {
    val pools = DbPoolMembership.findAll(By(DbPoolMembership.userId, user.id), By(DbPoolMembership.pool, this))
    val successMembership = pools.map(_.hasLeft(true).save()).reduce(_ && _)
    val invites = DbPoolInvites.findAll(By(DbPoolInvites.fbId, user.fbId), By(DbPoolInvites.pool, this))
    val successInvites = invites.map(_.ignored(true).save()).foldLeft(true)(_ && _)
    return successInvites && successMembership
  }

  def addUser(user: User): Boolean = {
    if (!_allowMemberInvite.is && !userIsInvited(user.fbId)) {
      warn("Trying to add user without invitation!")
      return false
    }
    val membershipBox = DbPoolMembership.find(By(DbPoolMembership.userId, user.id), By(DbPoolMembership.pool, this))
    val membership =  membershipBox.openOr(DbPoolMembership.create)
    membership.userId(user.id)
    membership.pool(this)
    membership.hasLeft(false)
    return membership.save()
  }

  lazy val users = {
    val memberships = DbPoolMembership.findAll(By(DbPoolMembership.pool, this), By(DbPoolMembership.hasLeft, false))
    memberships.map(_.userId.is).toSet
  }

  override def userHasLeftGroup(userId: Long) = {
    DbPoolMembership.find(By(DbPoolMembership.pool, this), By(DbPoolMembership.userId, userId)).map(_.hasLeft.is)
  }

  override def userIsAllowedToInvite(user: User) = _allowMemberInvite.is || (user.id == _adminId.is)

  override def inviteUser(facebookId: String, fromUser: Option[User]) = {
    val invitingUserId = fromUser match {
      case Some(user) => {
        if (!userIsAllowedToInvite(user)) {
          throw new IllegalArgumentException("Inviting user is not allowed to make invitations!")
        }
        user.id
      }
      case None => 0
    }
    val inviteBox = DbPoolInvites.find(By(DbPoolInvites.pool, this),
                                       By(DbPoolInvites.fbId, facebookId),
                                       By(DbPoolInvites.invitingUserId, invitingUserId))
    val invite = inviteBox openOr DbPoolInvites.create
    invite.invitingUserId(invitingUserId)
    invite.fbId(facebookId)
    invite.pool(this)
    invite.ignored(false)
    invite.save()
  }
  override def userIsInvited(facebookId: String) = {
    (DbPoolInvites.count(By(DbPoolInvites.pool, this), By(DbPoolInvites.fbId, facebookId)) > 0)
  }
  override def ignoreInvitations(user: User) = {
    val invitations = DbPoolInvites.findAll(By(DbPoolInvites.pool, this), By(DbPoolInvites.fbId, user.fbId))
    invitations.map { invitation =>
      invitation.ignored(true)
      invitation.save()
    }
  }

  override def updateDescription(description: String) = {
    _description(description)
    save()
  }
}

object DbInvitationLink extends DbInvitationLink with KeyedMetaMapper[String, DbInvitationLink]

class DbInvitationLink extends InvitationLink with KeyedMapper[String, DbInvitationLink] {
  override def getSingleton = DbInvitationLink
  override def primaryKeyField = _invitationId

  object _invitationId extends MappedStringIndex(this, 32) {
    override def dbPrimaryKey_? = true
    override def writePermission_? = true
    override def dbAutogenerated_? = false
  }
  object _userId extends MappedLong(this) {
    override def dbIndexed_? = true
  }
  object _pool extends MappedLongForeignKey(this, DbPool)

  override def invitationId = _invitationId.is
  override def poolId = _pool.is
  override def userId = _userId.is
}


object DbPoolMembership extends DbPoolMembership with LongKeyedMetaMapper[DbPoolMembership]

class DbPoolMembership extends LongKeyedMapper[DbPoolMembership] with IdPK {
  def getSingleton = DbPoolMembership

  object userId extends MappedLong(this) {
    override def dbIndexed_? = true
  }
  object pool extends MappedLongForeignKey(this, DbPool)
  object hasLeft extends MappedBoolean(this)
}

object DbPoolInvites extends DbPoolInvites with LongKeyedMetaMapper[DbPoolInvites]

class DbPoolInvites extends LongKeyedMapper[DbPoolInvites] with IdPK {
  def getSingleton = DbPoolInvites

  object invitingUserId extends MappedLong(this)
  object fbId extends MappedString(this, 16) {
    override def dbIndexed_? = true
  }
  object pool extends MappedLongForeignKey(this, DbPool)
  object ignored extends MappedBoolean(this)
}

object DbPoolComment extends DbPoolComment with LongKeyedMetaMapper[DbPoolComment] with MetaPoolComment {
  override def saveComment(pool: Pool, user: User, comment: String): Box[PoolComment] = {
    if (comment.length > 2048) {
      return Failure("Comment too long!")
    }
    val pc = DbPoolComment.create
    pc._userId(user.id)
    pc._poolId(pool.id)
    pc._date(new Date())
    pc._comment(comment)
    if (pc.save()) {
      Full(pc)
    } else {
      Failure("Failed to save tip!")
    }
  }

  override def commentsForPool(pool: Pool) = {
    val all = DbPoolComment.findAll(By(_poolId, pool.id))
    all.sortWith((e1, e2) => (e1.commentDate compareTo e2.commentDate) < 0)
  }
}

class DbPoolComment extends LongKeyedMapper[DbPoolComment] with IdPK with PoolComment {
  def getSingleton = DbPoolComment

  object _poolId extends MappedLong(this)
  object _date extends MappedDateTime(this)
  object _userId extends MappedLong(this)
  object _comment extends MappedString(this, 2048)

  override def commentDate = new DateTime(_date.is)
  override def commentId = id.is
  override def poolId = _poolId.is
  override def userId = _userId.is
  override def comment = _comment.is
}
