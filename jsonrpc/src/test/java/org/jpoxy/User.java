package org.jpoxy;

public class User {
    public enum Gender { MALE, FEMALE };

    public static class Name {
      private String _first, _last;

      public String getFirst() { return _first; }
      public String getLast() { return _last; }

      public void setFirst(String s) { _first = s; }
      public void setLast(String s) { _last = s; }
    }

    private Gender _gender;
    private Name _name;
    private String _alias;
    private int _age;
    private boolean _isVerified;
    private byte[] _userImage;

    public Name getName() { return _name; }
    public String getAlias() { return _alias; }
    public int getAge() { return _age; }
    public boolean isVerified() { return _isVerified; }
    public Gender getGender() { return _gender; }
    public byte[] getUserImage() { return _userImage; }

    public void setName(Name n) { _name = n; }
    public void setAlias(String a) { _alias = a; }
    public void setAge(int a) { _age = a; }
    public void setVerified(boolean b) { _isVerified = b; }
    public void setGender(Gender g) { _gender = g; }
    public void setUserImage(byte[] b) { _userImage = b; }
    public void setFullname(String fn) {
        Name name = new Name();
        String[] namechunks = fn.split(" ");
        name.setFirst(namechunks[0]);
        name.setLast(namechunks[1]);
        _name = name;
    }
}