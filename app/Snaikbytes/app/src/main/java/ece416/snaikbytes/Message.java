package ece416.snaikbytes;

/**
 * Created by mcoppola on 29/03/17.
 */

public class Message {

    String mUser;
    String mMessageContents;

    public Message(String user, String contents)
    {
        mUser = user;
        mMessageContents = contents;
    }

    public String GetUser()
    {
        return mUser;
    }

    public String GetMessage()
    {
        return  mMessageContents;
    }
}
