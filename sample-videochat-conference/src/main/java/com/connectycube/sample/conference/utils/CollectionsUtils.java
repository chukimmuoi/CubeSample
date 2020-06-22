package com.connectycube.sample.conference.utils;

import com.connectycube.core.helper.StringifyArrayList;
import com.connectycube.users.model.ConnectycubeUser;

import java.util.ArrayList;
import java.util.Collection;


public class CollectionsUtils {

    public static String makeStringFromUsersFullNames(ArrayList<ConnectycubeUser> allUsers) {
        StringifyArrayList<String> usersNames = new StringifyArrayList<>();

            for (ConnectycubeUser usr : allUsers) {
                if (usr.getFullName() != null) {
                    usersNames.add(usr.getFullName());
                } else if (usr.getId() != null) {
                    usersNames.add(String.valueOf(usr.getId()));
                }
            }
        return usersNames.getItemsAsString().replace(",",", ");
    }

    public static ArrayList<Integer> getIdsSelectedOpponents(Collection<ConnectycubeUser> selectedUsers){
        ArrayList<Integer> opponentsIds = new ArrayList<>();
        if (!selectedUsers.isEmpty()){
            for (ConnectycubeUser user : selectedUsers){
                opponentsIds.add(user.getId());
            }
        }

        return opponentsIds;
    }
}
