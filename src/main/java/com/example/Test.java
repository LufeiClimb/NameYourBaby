package com.example;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.entity.ContentType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test
 *
 * @author lufeixia
 * @version 1.0
 * @date 2020/3/13 12:21
 * @since 1.8
 */
public class Test {

    public static void main(String[] args) {
        String aaa = "昊泽浩然品源辰鑫思文涵卫轩梓译奕凡";

        char[] chars = aaa.toCharArray();
        List<JSONObject> names = new ArrayList<>();
        for (char aChar : chars) {
            for (char c : chars) {

                JSONObject map = new JSONObject();
                map.put("名字", "卢" + aChar + c);

                JSONObject param = new JSONObject();
                param.put("xs", "卢");
                param.put("mz", "" + "浩泽");
                param.put("action", "test");

                Map<String, String> headers = new HashMap<>();
                headers.put(
                        "cookie", "ffqm_uid=2437625; ffqm_sign=fda49225223242a2f577c7695ebfd531");
                headers.put(
                        "user-agent",
                        "Mozilla;5.0 (Linux; Android 9; Redmi Note 5 Build;PKQ1.180904.001; wv) AppleWebKit;537.36 (KHTML, like Gecko) Version;4.0 Chrome;74.0.3729.157 Mobile Safari;537.36;{qm-android:868773036592999};{versionCode:2.5.2};{extendid:0};{qm-android:868773036592999};{versionCode:2.5.2};{extendid:0}");
                try {
                    String total =
                            HttpUtil.httpsPost(
                                    "https://name2.feifanqiming.com/home/jieming_detail.html?f=%E5%8D%A2&s="
                                            + aChar
                                            + c
                                            + "&b=2020-02-17%2015:00&sex=2&bhour=&from=home",
                                    param,
                                    headers,
                                    ContentType.TEXT_HTML);
                    Document totalDoc = Jsoup.parse(total);
                    Elements small = totalDoc.select("div[class=col]");
                    String wuxing = "";
                    for (Element element : small) {
                        wuxing = wuxing + element.text().split("\\[")[1].split("]")[0];
                    }
                    map.put("五行", wuxing);
                    Elements s = totalDoc.getElementsByClass("rank-bar");
                    map.put("综合分数", s.text().replaceAll("[综合分数：]", ""));
                    Elements s1 = totalDoc.getElementsByClass("list");
                    for (Element element : s1) {
                        String[] a = element.text().split("分 ");
                        for (String s2 : a) {
                            String[] split = s2.replace("分", "").split(" ");
                            map.put(split[0], split[1]);
                        }
                    }
                    String wuge =
                            HttpUtil.httpsPost(
                                    "https://name2.feifanqiming.com/home/jieming_sancai.html?f=%E5%8D%A2&s="
                                            + aChar
                                            + c
                                            + "&b=2020-02-17%2015:00&sex=2&bhour=&from=home",
                                    param,
                                    headers,
                                    ContentType.TEXT_HTML);
                    Document wugeDoc = Jsoup.parse(wuge);
                    String[] s2 = wugeDoc.select("div[class=title]").text().split(" ");

                    map.put("三才", s2[0]);
                    map.put("总格", s2[1]);
                    map.put("天格", s2[2]);
                    map.put("人格", s2[3]);
                    map.put("地格", s2[4]);
                    map.put("外格", s2[5]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                names.add(map);
                break;
            }
            break;
        }
        System.out.println(names);

        String sheetName = "备用名";
        String[] title = {
            "名字", "五行", "综合分数", "字形意义", "生肖喜忌", "八字喜用", "三才五格", "三才", "总格", "天格", "人格", "地格", "外格"
        };
        try {
            FileInputStream fileInputStream =
                    new FileInputStream("C:\\Users\\lufeixia\\Desktop\\宝宝起名.xls");
            HSSFWorkbook wb =
                    ExcelUtil.getHSSFWorkbook(
                            sheetName, title, names, new HSSFWorkbook(fileInputStream));
            FileOutputStream output =
                    new FileOutputStream("C:\\Users\\lufeixia\\Desktop\\宝宝起名1.xlsx");
            wb.write(output);
            output.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
