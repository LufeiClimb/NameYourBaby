package com.example.demo;

import com.alibaba.fastjson.JSONObject;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.HorizontalAlignment;

import java.util.List;
import java.util.regex.Pattern;

public class ExcelUtil {

    /**
     * 导出Excel
     *
     * @param sheetName sheet名称
     * @param title     标题
     * @param values    内容
     * @param wb        HSSFWorkbook对象
     * @return
     */
    public static HSSFWorkbook getHSSFWorkbook(String sheetName, String[] title, List<JSONObject> values, HSSFWorkbook wb) {

        // 第一步，创建一个HSSFWorkbook，对应一个Excel文件
        if (wb == null) {
            wb = new HSSFWorkbook();
        }

        // 第二步，在workbook中添加一个sheet,对应Excel文件中的sheet
        HSSFSheet sheet = wb.getSheet(sheetName);
        if (sheet == null) {
            sheet = wb.createSheet(sheetName);
        }
        // 第三步，在sheet中添加表头第0行,注意老版本poi对Excel的行数列数有限制
        HSSFRow row;

        // 第四步，创建单元格，并设置值表头 设置表头居中
        HSSFCellStyle style = wb.createCellStyle();
        // 创建一个居中格式
        style.setAlignment(HorizontalAlignment.CENTER);

        //声明列对象
        HSSFCell cell = null;

        // //创建标题
        // for (int i = 0; i < title.length; i++) {
        //     cell = row.createCell(i);
        //     cell.setCellValue(title[i]);
        //     cell.setCellStyle(style);
        // }

        //创建内容
        for (int i = 0; i < values.size(); i++) {
            row = sheet.createRow(i + 17);
            for (int j = 0; j < title.length; j++) {

                String value = values.get(i).getString(title[j] + "");

                Pattern pattern = Pattern.compile("^(-?\\d+)(\\.\\d+)?$");
                boolean bo=pattern.matcher(value).matches();
                if(bo){
                    row.createCell(j).setCellValue(Double.valueOf(value));
                } else {
                    row.createCell(j).setCellValue(value);
                }

            }
        }
        return wb;
    }
}