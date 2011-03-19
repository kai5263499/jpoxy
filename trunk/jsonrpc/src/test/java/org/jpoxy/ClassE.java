package org.jpoxy;

public class ClassE {

    public ClassE() {
    }

    public User edit(User user) {
        user.setAlias("Neo");
        user.setAge(user.getAge()-20);
        return user;
    }

    @Jpoxy(enabled = false)
    public boolean isTooOld() {
        return false;
    }
}
