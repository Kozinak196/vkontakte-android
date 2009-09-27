package org.googlecode.vkontakte_android.database;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import org.googlecode.userapi.User;
import org.googlecode.vkontakte_android.provider.UserapiDatabaseHelper;
import static org.googlecode.vkontakte_android.provider.UserapiDatabaseHelper.*;
import org.googlecode.vkontakte_android.provider.UserapiProvider;
import static org.googlecode.vkontakte_android.provider.UserapiProvider.USERS_URI;

import java.util.LinkedList;
import java.util.List;

public class UserDao extends org.googlecode.userapi.User {
    private static final String TAG = "org.googlecode.vkontakte_android.database.UserDao";


    private long rowId;
    //    private String userPhotoUrl;
    //    private String userPhotoUrlSmall;
    private long photo;
    private long photoSmall;
    private boolean newFriend;
    //todo: save urls/full avatars

    public UserDao(Cursor cursor) {
        rowId = cursor.getLong(0);
        userId = cursor.getLong(1);
        userName = cursor.getString(2);
        male = cursor.getInt(3) == 1;
        online = cursor.getInt(4) == 1;
        newFriend = cursor.getInt(5) == 1;
    }

    public UserDao(long userId, String userName, boolean male, boolean online, boolean newFriend) {
        this.userId = userId;
        this.userName = userName;
        this.male = male;
        this.online = online;
        this.newFriend = newFriend;
    }

    public static int bulkSave(Context context, List<UserDao> userListDao) {
        ContentValues[] values = new ContentValues[userListDao.size()];
        int i = 0;
        for (UserDao userDao : userListDao) {
            ContentValues insertValues = new ContentValues();
            insertValues.put(KEY_USER_USERID, userDao.getUserId());
            insertValues.put(KEY_USER_NAME, userDao.getUserName());
            insertValues.put(KEY_USER_MALE, userDao.isMale() ? 1 : 0);
            insertValues.put(KEY_USER_ONLINE, userDao.isOnline() ? 1 : 0);
            insertValues.put(KEY_USER_NEW, userDao.isNewFriend() ? 1 : 0);
            values[i] = insertValues;
            i++;
        }
        return context.getContentResolver().bulkInsert(USERS_URI, values);
    }

    public static int[] bulkUpdateOrRemove(Context context, List<UserDao> users) {
        int added = 0;
        int removed;
        List<Long> oldUsers = UserDao.getAllOldFriendsId(context);
        for (User user : users) {
            System.out.println("updating " + user.getUserId());
            UserDao userDao = new UserDao(user.getUserId(), user.getUserName(), user.isMale(), user.isOnline(), false);
            added += userDao.saveOrUpdate(context);
            oldUsers.remove(userDao.getUserId());
        }
        for (Long id : oldUsers) {
            System.out.println("deleting " + id);
            context.getContentResolver().delete(USERS_URI, KEY_USER_USERID + "=?", new String[]{String.valueOf(id)});
        }
        removed = oldUsers.size();
        context.getContentResolver().notifyChange(UserapiProvider.USERS_URI, null);
        return new int[]{removed, added};
    }

    public static List<Long> getAllOldFriendsId(Context context) {
        List<Long> ids = new LinkedList<Long>();
        Cursor cursor = context.getContentResolver().query(USERS_URI, new String[]{KEY_USER_USERID}, UserapiDatabaseHelper.KEY_USER_NEW + "=?", new String[]{String.valueOf(0)}, null);
        if (cursor != null) {
            while (cursor.moveToNext())
                ids.add(cursor.getLong(0));
            cursor.close();
        }
        return ids;
    }

    public static UserDao get(Context context, long rowId) {
        if (rowId == -1) return null;
        Cursor cursor = context.getContentResolver().query(ContentUris.withAppendedId(USERS_URI, rowId), null, null, null, null);
        UserDao userDao = null;
        if (cursor != null && cursor.moveToNext()) {
            userDao = new UserDao(cursor);
            cursor.close();
        } else if (cursor != null) cursor.close();
        return userDao;
    }

    public static UserDao findByUserId(Context context, long id) {
        if (id == -1) return null;
//        Cursor cursor = context.getContentResolver().query(USERS_URI, null, KEY_USER_USERID + "=?", new String[]{String.valueOf(id)}, null);
        Cursor cursor = context.getContentResolver().query(USERS_URI, null, KEY_USER_USERID + "=?" + " AND " + KEY_USER_NEW + "=?", new String[]{String.valueOf(id), String.valueOf(0)}, null);
        UserDao userDao = null;
        if (cursor != null && cursor.moveToNext()) {
            userDao = new UserDao(cursor);
            cursor.close();
        } else if (cursor != null) cursor.close();
        return userDao;
    }

    public int saveOrUpdate(Context context) {
        UserDao channel = UserDao.findByUserId(context, userId);
        ContentValues insertValues = new ContentValues();
        insertValues.put(KEY_USER_USERID, getUserId());
        insertValues.put(KEY_USER_NAME, getUserName());
        insertValues.put(KEY_USER_MALE, isMale() ? 1 : 0);
        insertValues.put(KEY_USER_ONLINE, isOnline() ? 1 : 0);
        insertValues.put(KEY_USER_NEW, isNewFriend() ? 1 : 0);
        if (isNewFriend()) System.out.println("new!");
        else System.out.println("old :(");
        if (channel == null) {
            context.getContentResolver().insert(USERS_URI, insertValues);
            return 1;
        } else {
            context.getContentResolver().update(ContentUris.withAppendedId(USERS_URI, channel.rowId), insertValues, null, null);
            return 0;
        }
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setMale(boolean male) {
        this.male = male;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isNewFriend() {
        return newFriend;
    }

    public long getRowId() {
        return rowId;
    }
}