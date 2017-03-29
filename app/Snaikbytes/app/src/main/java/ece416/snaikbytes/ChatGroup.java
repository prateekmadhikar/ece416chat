package ece416.snaikbytes;

import android.util.Pair;

import java.util.Vector;

/**
 * Created by mcoppola on 29/03/17.
 */

public class ChatGroup {

    Vector<Message> mMessages;
    String mGroupID;

    public ChatGroup(String id)
    {
        mGroupID = id;
        mMessages = new Vector<Message>();
    }

    public Vector<Message> GetMessages()
    {
        return mMessages;
    }

    public  void AddChat(String user, String message)
    {
        mMessages.add(new Message(user, message));
    }
}
