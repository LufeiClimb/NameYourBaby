package com.example.demo;

import com.alibaba.fastjson.JSONObject;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Test
 *
 * @author lufeixia
 * @version 1.0
 * @date 2020/3/13 12:21
 * @since 1.8
 */
@RestController
@RequestMapping("")
@Api
public class Test {

    public static void main(String[] args) {
        Test test = new Test();
        try {
            test.my("昊泽浩然品玥宇宏佑赫源辰鑫思文涵卫轩梓新译俊跃杰奕凡延纬靳笙晓童钧沐景煜恺", true, false, "然鑫译文涵凡童钧景宇杰", "思品梓奕凡浩昊");
            // test.suiji(true, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/my")
    @ApiOperation(value = "my")
    public void my(
            @RequestParam @ApiParam(defaultValue = "昊泽浩然品玥宇宏佑赫源辰鑫思文涵卫轩梓新译俊跃杰奕凡延纬靳笙晓童钧沐景煜恺")
                    String words,
            @RequestParam @ApiParam(defaultValue = "true") boolean wuxing,
            @RequestParam @ApiParam(defaultValue = "false") boolean xiongPaichu,
            @RequestParam @ApiParam(defaultValue = "然鑫译文涵凡童钧景宇杰") String erPaiChu,
            @RequestParam @ApiParam(defaultValue = "思品梓奕凡浩昊") String sanPaiChu)
            throws ExecutionException, InterruptedException, IOException {
        char[] chars = words.toCharArray();
        List<JSONObject> names = new ArrayList<>();
        List<String> allName = new ArrayList<>();
        List<String> wordsList = new ArrayList<>();
        for (char aChar : chars) {
            wordsList.add(String.valueOf(aChar));
        }

        List<String> excelNames = getNameFromExcel();

        for (String worda : wordsList) {
            if (!erPaiChu.contains(worda)) {
                for (String wordb : wordsList) {
                    if (!sanPaiChu.contains(wordb)) {
                        String name = "卢" + worda + wordb;
                        if (!excelNames.contains(name)) {
                            allName.add(name);
                        }
                    }
                }
            }
        }

        packageExcelValue(wuxing, xiongPaichu, names, allName);

        exportToExcel(names, excelNames);
    }

    @GetMapping("/suiji")
    @ApiOperation(value = "suiji")
    public void suiji(
            @RequestParam @ApiParam(defaultValue = "true") boolean wuxing,
            @RequestParam @ApiParam(defaultValue = "false") boolean xiongPaichu)
            throws ExecutionException, InterruptedException, IOException {
        List<JSONObject> names = new ArrayList<>();

        List<String> allName = new ArrayList<>();

        List<String> excelNames = getNameFromExcel();

        String httpUrl = "https://www.qmsjmfb.com/";
        JSONObject param = new JSONObject();
        param.put("xing", "卢");
        param.put("xinglength", 1);
        param.put("minglength", "2");
        param.put("sex", "nan");
        param.put("dic", "0010"); // default 3040 5060 8090 0010 gudai ganzhi
        param.put("num", "200");

        String formResult = HttpUtil.httpForm(httpUrl, param);
        Document totalDoc = Jsoup.parse(formResult);
        Elements small = totalDoc.select("ul[class=name_show]").select("li");
        for (Element element : small) {
            String name = element.toString().replaceAll("[</li>]", "");
            if (!excelNames.contains(name)) {
                allName.add(name);
            }
        }

        packageExcelValue(wuxing, xiongPaichu, names, allName);

        exportToExcel(names, excelNames);
    }

    private void packageExcelValue(
            boolean wuxing, boolean xiongPaichu, List<JSONObject> names, List<String> allName)
            throws InterruptedException, ExecutionException {

        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(
                        5, 10000, 200, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(5));

        List<Future<JSONObject>> results = new ArrayList<>();
        for (String name1 : allName) {
            Thread.sleep(10);
            MyTask myTask = new MyTask(wuxing, xiongPaichu, name1);
            Future<JSONObject> submit = executor.submit(myTask);
            results.add(submit);
        }
        Set<Integer> sss = new HashSet<>();
        do {
            //            System.out.printf("number of completed tasks: %d\n",
            // executor.getCompletedTaskCount());
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

    private void exportToExcel(List<JSONObject> names, List<String> excelNames) {
        FileInputStream fileInputStream;
        String sheetName = "备用名";
        String[] title = {
                "名字", "五行", "综合分数", "字形意义", "生肖喜忌", "八字喜用", "三才五格", "三才", "总格", "天格", "人格", "地格", "外格"
        };
        JSONObject now = new JSONObject();
        now.put("名字", "卢" + LocalDateTime.now().toString());
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
        private boolean wuxing;
        private boolean xiongPaichu;
        private String name;

        public MyTask(boolean wuxing, boolean xiongPaichu, String name) {
            this.taskName = wuxing + name;
            this.wuxing = wuxing;
            this.xiongPaichu = xiongPaichu;
            this.name = name;
        }

        @Override
        public JSONObject call() {
            JSONObject jsonObject = new JSONObject();
            // System.out.println("正在执行task " + taskName);
            try {
                jsonObject = doTask(wuxing, xiongPaichu, name);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // System.out.println("task " + taskName + "执行完毕");
            return jsonObject;
        }

        private JSONObject doTask(boolean wuxing, boolean xiongPaichu, String name1) {
            JSONObject map = new JSONObject();
            map.put("名字", name1);

            if (wuxing) {
                JSONObject param = new JSONObject();
                param.put("xs", "卢");
                param.put("mz", name1.replace("卢", ""));
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
                                            + name1.replace("卢", "")
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
                        wuxing1 = wuxing1 + element.text().split("\\[")[1].split("]")[0];
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
                                            + name1.replace("卢", "")
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
                }
            }
            return map;
        }
    }
}
