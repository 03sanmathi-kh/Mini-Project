package edu.kle.permits;

/** A provider returns a delivery state; local mock never sends network messages. */
public interface SmsProvider { String send(Long recipientId,String event); }
