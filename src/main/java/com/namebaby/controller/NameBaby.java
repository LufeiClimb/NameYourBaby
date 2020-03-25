package com.namebaby.controller;

import com.alibaba.fastjson.JSONObject;
import com.namebaby.utils.ExcelUtil;
import com.namebaby.utils.HttpUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * NameBaby
 *
 * @author lufeixia
 * @version 1.0
 * @date 2020/3/13 12:21
 * @since 1.8
 */
@Api(value = "/nameBaby")
@RestController
@RequestMapping("/nameBaby")
public class NameBaby {

    public static void main(String[] args) {
        NameBaby test = new NameBaby();
        try {
            test.byWords(
                    "卢",
                    "钰昊心兴阳羽凯宏伟辉凌霄明彦晏筱颜瑞文一贤其烁君博天熙泽浩逸汝云柏川铭子帅伟璟旭东乐灏征朗涵晗奇星柯喆哲然品玥宇宏佑赫源辰鑫思文涵卫轩梓新译俊跃杰奕凡延纬靳笙晓童钧沐景煜恺",
                    true,
                    false,
                    "然鑫译文涵凡童钧景宇杰",
                    "思品梓奕凡浩昊");
            // test.fromNet(true, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/byWords")
    @ApiOperation(value = "自定义文字生成名字")
    public Integer byWords(
            @RequestParam @ApiParam(required = true, value = "姓") String surname,
            @RequestParam
                    @ApiParam(
                            required = true, defaultValue =
                                    "钰昊心兴阳羽凯宏伟辉凌霄明彦晏筱颜瑞文一贤其烁君博天熙泽浩逸汝云柏川铭子帅伟璟旭东乐灏征朗涵晗奇星柯喆哲然品玥宇宏佑赫源辰鑫思文涵卫轩梓新译俊跃杰奕凡延纬靳笙晓童钧沐景煜恺")
                    String words,
            @RequestParam @ApiParam(defaultValue = "true", value = "是否计算五行") boolean wuxing,
            @RequestParam @ApiParam(defaultValue = "false", value = "排除三才五格为凶的名字")
                    boolean xiongPaiChu,
            @RequestParam @ApiParam(defaultValue = "然鑫译文涵凡童钧景宇杰", value = "排除第二个字为这些字的名字")
                    String erPaiChu,
            @RequestParam @ApiParam(defaultValue = "思品梓奕凡浩昊", value = "排除第三个字为这些字的名字")
                    String sanPaiChu)
            throws ExecutionException, InterruptedException, IOException {
        char[] chars = words.toCharArray();
        List<String> wordsList = new ArrayList<>();
        for (char aChar : chars) {
            wordsList.add(String.valueOf(aChar));
        }

        List<String> excelNames = getNameFromExcel();
        List<String> allName = new ArrayList<>();

        for (String worda : wordsList) {
            if (!erPaiChu.contains(worda)) {
                for (String wordb : wordsList) {
                    if (!sanPaiChu.contains(wordb)) {
                        String name = surname + worda + wordb;
                        if (!excelNames.contains(name)) {
                            allName.add(name);
                        }
                    }
                }
            }
        }

        List<JSONObject> names = new ArrayList<>();

        packageExcelValue(surname, wuxing, xiongPaiChu, names, allName);

        exportToExcel(surname, names, excelNames);
        return names.size();
    }

    @GetMapping("/fromNet")
    @ApiOperation(value = "从 www.qmsjmfb.com 网站上生成")
    public Integer fromNet(
            @RequestParam @ApiParam(required = true, value = "姓") String surname,
            @RequestParam @ApiParam(required = true, defaultValue = "nan", value = "性别", allowableValues = "nan, nv") String sex,
            @RequestParam @ApiParam(defaultValue = "true", value = "是否计算五行") boolean wuxing,
            @RequestParam @ApiParam(defaultValue = "false", value = "排除三才五格为凶的名字")
                    boolean xiongPaichu)
            throws ExecutionException, InterruptedException, IOException {

        List<String> allName = new ArrayList<>();
        List<String> excelNames = getNameFromExcel();

        String httpUrl = "https://www.qmsjmfb.com/";
        JSONObject param = new JSONObject();
        param.put("xing", surname);
        param.put("xinglength", "all");
        param.put("minglength", "all");
        param.put("sex", sex);
        param.put("dic", "default"); // default 3040 5060 8090 0010 gudai ganzhi
        param.put("num", "2000");
        String formResult = HttpUtil.httpForm(httpUrl, param);

        Document totalDoc = Jsoup.parse(formResult);
        Elements small = totalDoc.select("ul[class=name_show]").select("li");
        for (Element element : small) {
            String name = element.toString().replaceAll("[</li>]", "");
            if (!excelNames.contains(name)) {
                allName.add(name);
            }
        }

        List<JSONObject> names = new ArrayList<>();

        packageExcelValue(surname, wuxing, xiongPaichu, names, allName);

        exportToExcel(surname, names, excelNames);
        return names.size();
    }

    private List<String> getNameFromExcel() throws IOException {
        List<String> excelNames = new ArrayList<>();

        FileInputStream fileInputStream = new FileInputStream("宝宝起名.xls");
        Workbook wbRead = new HSSFWorkbook(fileInputStream);
        Sheet sheet = wbRead.getSheet("备用名");
        int totalNum = sheet.getPhysicalNumberOfRows();
        for (int j = 0; j < totalNum; j++) {
            HSSFRow row = (HSSFRow) sheet.getRow(j);
            HSSFCell cell = row.getCell(0);
            String stringCellValue = cell.getStringCellValue();
            excelNames.add(stringCellValue);
        }
        return excelNames;
    }

    private void packageExcelValue(
            String surname,
            boolean wuxing,
            boolean xiongPaichu,
            List<JSONObject> names,
            List<String> allName)
            throws InterruptedException, ExecutionException {

        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(
                        5, 10000, 200, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(5));

        List<Future<JSONObject>> results = new ArrayList<>();
        for (String name1 : allName) {
            Thread.sleep(10);
            MyTask myTask = new MyTask(surname, wuxing, xiongPaichu, name1);
            Future<JSONObject> submit = executor.submit(myTask);
            results.add(submit);
        }
        Set<Integer> sss = new HashSet<>();
        do {
            for (int j = 0; j < results.size(); j++) {
                Future<JSONObject> result = results.get(j);
                if (result.isDone() && !sss.contains(j)) {
                    sss.add(j);
                    System.out.println(sss.size() + "/" + results.size() + "  ");
                }
            }

        } while (executor.getCompletedTaskCount() < results.size());

        executor.shutdown();

        for (Future<JSONObject> result : results) {
            JSONObject jsonObject = result.get();
            if (jsonObject != null) {
                names.add(result.get());
            }
        }
        System.out.println(names);
    }

    private void exportToExcel(String surname, List<JSONObject> names, List<String> excelNames) {
        FileInputStream fileInputStream;
        String sheetName = "备用名";
        String[] title = {
            "名字", "五行", "综合分数", "字形意义", "生肖喜忌", "八字喜用", "三才五格", "三才", "总格", "天格", "人格", "地格", "外格"
        };
        JSONObject now = new JSONObject();
        now.put("名字", surname);
        names.add(0, now);
        try {
            fileInputStream = new FileInputStream("宝宝起名.xls");
            HSSFWorkbook wb =
                    ExcelUtil.getHSSFWorkbook(
                            sheetName,
                            title,
                            names,
                            new HSSFWorkbook(fileInputStream),
                            excelNames.size());
            FileOutputStream output = new FileOutputStream("宝宝起名.xls");
            wb.write(output);
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class MyTask implements Callable<JSONObject> {
        private String taskName;
        private String surname;
        private boolean wuxing;
        private boolean xiongPaichu;
        private String name;

        MyTask(String surname, boolean wuxing, boolean xiongPaichu, String name) {
            this.taskName = wuxing + name;
            this.surname = surname;
            this.wuxing = wuxing;
            this.xiongPaichu = xiongPaichu;
            this.name = name;
        }

        @Override
        public JSONObject call() {
            JSONObject jsonObject = new JSONObject();
            // System.out.println("正在执行task " + taskName);
            try {
                jsonObject = doTask(surname, wuxing, xiongPaichu, name);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // System.out.println("task " + taskName + "执行完毕");
            return jsonObject;
        }

        private JSONObject doTask(
                String surname, boolean wuxing, boolean xiongPaichu, String name) {
            JSONObject map = new JSONObject();
            map.put("名字", name);

            if (wuxing) {
                JSONObject param = new JSONObject();
                param.put("xs", surname);
                param.put("mz", name.replace(surname, ""));
                param.put("action", "test");

                Map<String, String> headers = new HashMap<>();
                headers.put(
                        "cookie", "ffqm_uid=2437625; ffqm_sign=fda49225223242a2f577c7695ebfd531");
                headers.put(
                        "user-agent",
                        "Mozilla;5.0 (Linux; Android 9; Redmi Note 5 Build;PKQ1.180904.001; wv) AppleWebKit;"
                                + "537.36 (KHTML, like Gecko) Version;4.0 Chrome;74.0.3729.157 Mobile Safari;"
                                + "537.36;{qm-android:868773036592999};{versionCode:2.5.2};{extendid:0};"
                                + "{qm-android:868773036592999};{versionCode:2.5.2};{extendid:0}");
                try {
                    String total =
                            HttpUtil.httpsPost(
                                    "https://name2.feifanqiming.com/home/jieming_detail.html?f=%E5%8D%A2&s="
                                            + name.replace(surname, "")
                                            + "&b=2020-02-17%2015:00&sex=2&bhour=&from=home",
                                    param,
                                    headers,
                                    ContentType.TEXT_HTML);
                    if (StringUtils.isBlank(total)) {
                        return null;
                    }
                    Document totalDoc = Jsoup.parse(total);
                    Elements small = totalDoc.select("div[class=col]");
                    String wuxing1 = "";
                    for (Element element : small) {
                        try {
                            wuxing1 = wuxing1 + element.text().split("\\[")[1].split("]")[0];
                        } catch (Exception e) {
                            wuxing1 = "";
                        }
                    }
                    map.put("五行", wuxing1);
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
                                            + name.replace(surname, "")
                                            + "&b=2020-02-17%2015:00&sex=2&bhour=&from=home",
                                    param,
                                    headers,
                                    ContentType.TEXT_HTML);
                    if (StringUtils.isBlank(wuge)) {
                        return null;
                    }
                    Document wugeDoc = Jsoup.parse(wuge);
                    String text = wugeDoc.select("div[class=title]").text();
                    if (xiongPaichu && text.contains("凶")) {
                        return null;
                    }
                    String[] s2 = text.split(" ");

                    map.put("三才", s2[0].replace("三才", ""));
                    map.put("总格", s2[1].replace("总格", ""));
                    map.put("天格", s2[2].replace("天格", ""));
                    map.put("人格", s2[3].replace("人格", ""));
                    map.put("地格", s2[4].replace("地格", ""));
                    map.put("外格", s2[5].replace("外格", ""));
                } catch (IOException e) {
                    e.printStackTrace();
                    return map;
                }
            }
            return map;
        }
    }
}
