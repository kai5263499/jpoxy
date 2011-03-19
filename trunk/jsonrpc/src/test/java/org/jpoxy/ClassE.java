package org.jpoxy;

public class ClassE {

    public ClassE() {
    }

    public User edit(User user) {

        System.out.println("in edit user method!! getName: "+user.getName()+" getAge: "+user.getAge());

        user.setName("Neo");
        user.setAge(22);
        return user;
    }
}
