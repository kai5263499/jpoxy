package org.jpoxy;

import org.jpoxy.annotate.JpoxyIgnore;

public class ClassE {

    public ClassE() {
    }

    public User edit(User user) {

        user.setAlias("Neo");
        user.setAge(user.getAge()-20);
        user.setMe(user); // Circular dependency!

        return user;
    }

    @JpoxyIgnore
    public boolean isTooOld() {
        return false;
    }
}
