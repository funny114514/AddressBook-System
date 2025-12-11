package com.example.backend.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
@ColumnWidth(20) // 设置统一列宽，好看一点
public class ContactExportVO {

    @ExcelProperty("ID")
    private Long id;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("地址")
    @ColumnWidth(30) // 地址通常比较长，宽一点
    private String address;

    @ExcelProperty("是否收藏")
    private String isFavorite; // 导出成 "是/否" 比 true/false 好看

    // === 下面是把原本的 List 拆解成具体的列 ===

    @ExcelProperty("手机")
    private String phone;

    @ExcelProperty("微信")
    private String wechat;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("其他联系方式")
    private String other;
}