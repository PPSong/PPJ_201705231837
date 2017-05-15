package com.penn.ppj.util;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.penn.ppj.PPApplication;
import com.penn.ppj.model.realm.CurrentUser;
import com.penn.ppj.model.realm.Pic;
import com.penn.ppj.ppEnum.PPValueType;
import com.penn.ppj.ppEnum.PicStatus;

import org.json.JSONObject;

import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;

/**
 * Created by penn on 13/05/2017.
 */

public class PPHelper {

    public static String currentUserId;
    public static String token;
    public static long tokenTimestamp;

    public static String currentUserNickname;
    public static String currentUserAvatar;
    public static String socketUrl;
    public static String baiduAk = "";

    public static int getStatusBarAddActionBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }

        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            result += TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return result;
    }

    public static JsonElement ppFromString(String json, String path, PPValueType type) {
        JsonElement jsonElement = ppFromString(json, path);
        if (jsonElement == null) {
            switch (type) {
                case ARRAY:
                    return new JsonArray();
                case INT:
                    return new JsonPrimitive(0);
                case STRING:
                    return new JsonPrimitive("");
                default:
                    return null;
            }
        }

        return jsonElement;
    }

    public static JsonElement ppFromString(String json, String path) {
        try {
            JsonParser parser = new JsonParser();
            JsonElement item = parser.parse(json);
            if (path == null || path.length() == 0 || Pattern.matches("\\.+", path)) {
                //Log.v("ppLog", "解析整个json String");
                return item;
            }
            String[] seg = path.split("\\.");
            for (int i = 0; i < seg.length; i++) {
                if (i > 0) {
                    //Log.v("ppLog", "解析完毕:" + seg[i - 1]);
                    //Log.v("ppLog", "-------");
                }
                //Log.v("ppLog", "准备解析:" + seg[i]);
                if (seg[i].length() == 0) {
                    //""情况
                    //Log.v("ppLog", "解析空字符串的path片段, 停止继续解析");
                    return null;
                }
                if (item != null) {
                    //当前path片段item不为null
                    //Log.v("ppLog", "当前path片段item不为null");
                    if (item.isJsonArray()) {
                        //当前path片段item为数组
                        //Log.v("ppLog", "当前path片段item为数组");
                        String regex = "\\d+";
                        if (Pattern.matches("\\d+", seg[i])) {
                            //当前path片段描述为数组格式
                            //Log.v("ppLog", "当前path片段描述为数组格式");
                            item = item.getAsJsonArray().get(Integer.parseInt(seg[i]));
                        } else {
                            //当前path片段描述不为数组格式
                            //Log.v("ppLog", "当前path片段描述不为数组格式");
                            //Log.v("ppLog", "path中间片段描述错误:" + seg[i] + ", 停止继续解析");
                            return null;
                        }
                    } else if (item.isJsonObject()) {
                        //当前path片段item为JsonObject
                        //Log.v("ppLog", "当前path片段item为JsonObject");
                        item = item.getAsJsonObject().get(seg[i]);
                    } else {
                        //当前path片段item为JsonPrimitive
                        //Log.v("ppLog", "当前path片段item为JsonPrimitive");
                        //Log.v("ppLog", "path中间片段取值为JsonPrimitive, 停止继续解析");
                        return null;
                    }
                } else {
                    //当前path片段item为null
                    //Log.v("ppLog", "当前path片段item为null");
                    Log.v("ppLog", path + ":path中间片段取值为null, 停止继续解析");
                    return null;
                }
            }
            return item;
        } catch (Exception e) {
            Log.v("ppLog", "Json解析错误" + e);
            return null;
        }
    }

    public static PPWarn ppWarning(String jServerResponse) {
        int code = ppFromString(jServerResponse, "code").getAsInt();
        if (code != 1) {
            return new PPWarn(jServerResponse);
        } else {
            return null;
        }
    }

    public static void initRealm(Context context, String phone) {
        Realm.init(context);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .name(phone + ".realm")
                .build();
        //清除当前用户的数据文件, 测试用
        boolean clearData = true;
        if (clearData) {
            Realm.deleteRealm(config);
        }

        Realm.setDefaultConfiguration(config);
    }

    public static Observable<String> login(final String phone, String pwd) throws Exception {
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("phone", phone)
                .put("pwd", pwd);

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("user.login", jBody.getJSONObject());

        return apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(String s) throws Exception {
                        PPWarn ppWarn = ppWarning(s);
                        if (ppWarn != null) {
                            throw new Exception(ppWarn.msg);
                        }

                        initRealm(PPApplication.getContext(), phone);
                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.beginTransaction();

                            CurrentUser currentUser = new CurrentUser();
                            currentUser.setUserId(ppFromString(s, "data.userid").getAsString());
                            currentUser.setToken(ppFromString(s, "data.token").getAsString());
                            currentUser.setTokenTimestamp(ppFromString(s, "data.tokentimestamp").getAsLong());

                            realm.copyToRealmOrUpdate(currentUser);

                            realm.commitTransaction();

                            //设置PPRetrofit authBody
                            String authBody = new JSONObject()
                                    .put("userid", currentUser.getUserId())
                                    .put("token", currentUser.getToken())
                                    .put("tokentimestamp", currentUser.getTokenTimestamp())
                                    .toString();
                            PPRetrofit.authBody = authBody;
                            currentUserId = currentUser.getUserId();
                            token = currentUser.getToken();
                            tokenTimestamp = currentUser.getTokenTimestamp();
                        }

                        return PPRetrofit.getInstance().api("user.startup", null);
                    }
                })
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String s) throws Exception {
                        PPWarn ppWarn = ppWarning(s);
                        if (ppWarn != null) {
                            throw new Exception(ppWarn.msg);
                        }

                        //pptodo connect to rongyun
                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.beginTransaction();

                            CurrentUser currentUser = realm.where(CurrentUser.class)
                                    .findFirst();

                            String tmpAk = ppFromString(s, "data.settings.geo.ak_browser").getAsString();

                            currentUser.setPhone(ppFromString(s, "data.userInfo.phone").getAsString());
                            currentUser.setNickname(ppFromString(s, "data.userInfo.nickname").getAsString());
                            currentUser.setGender(ppFromString(s, "data.userInfo.gender").getAsInt());
                            currentUser.setBirthday(ppFromString(s, "data.userInfo.birthday").getAsLong());
                            currentUser.setHead(ppFromString(s, "data.userInfo.head").getAsString());
                            currentUser.setBaiduApiUrl(ppFromString(s, "data.settings.geo.api").getAsString());
                            currentUser.setBaiduAkBrowser(tmpAk);
                            currentUser.setSocketHost(ppFromString(s, "data.settings.socket.host").getAsString());
                            currentUser.setSocketPort(ppFromString(s, "data.settings.socket.port").getAsInt());

                            //pptodo get im_unread_count_int
                            RealmList<Pic> pics = currentUser.getPics();
                            JsonArray tmpArr = ppFromString(s, "data.userInfo.params.more.pics", PPValueType.ARRAY).getAsJsonArray();
                            for (int i = 0; i < tmpArr.size(); i++) {
                                Pic pic = new Pic();
                                pic.setKey("profile_pic" + i);
                                pic.setNetFileName(tmpArr.get(i).getAsString());
                                pic.setStatus(PicStatus.NET);
                                pics.add(pic);
                            }

                            currentUserNickname = currentUser.getNickname();
                            currentUserAvatar = currentUser.getHead();
                            socketUrl = currentUser.getSocketHost() + ":" + currentUser.getSocketPort();

                            //设置baiduAk
                            baiduAk = tmpAk;
                            realm.commitTransaction();
                        }
                        return "OK";
                    }
                });
    }
}
