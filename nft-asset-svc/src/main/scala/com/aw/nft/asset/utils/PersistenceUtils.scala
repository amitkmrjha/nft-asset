package com.aw.nft.asset.utils

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.persistence.typed.scaladsl.{Effect, ReplyEffect}

trait PersistenceUtils:

  protected def persistEventAndAck[E, S](evt: E, replyTo: ActorRef[StatusReply[Done]]): ReplyEffect[E, S] =
    Effect
      .persist(evt)
      .thenReply(replyTo)(_ => StatusReply.Ack)

  protected def persistAndReply[E, S, R](
      evt: E,
      replyTo: ActorRef[StatusReply[R]],
      reply: R
  ): ReplyEffect[E, S] =
    Effect
      .persist(evt)
      .thenReply(replyTo)(_ => StatusReply.success(reply))
