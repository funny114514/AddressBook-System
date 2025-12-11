package com.example.backend.controller;

import com.alibaba.excel.EasyExcel;
import com.example.backend.entity.Contact;
import com.example.backend.entity.ContactDetail;
import com.example.backend.entity.ContactExportVO;
import com.example.backend.repository.ContactRepository;
import com.example.backend.service.ContactListener;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/contact")
@CrossOrigin
public class ContactController {

    @Autowired
    private ContactRepository contactRepository;

    @GetMapping("/list")
    public List<Contact> list() {
        return contactRepository.findAll();
    }

    // ============ 核心修改：保存逻辑 ============
    @PostMapping("/save")
    public Contact save(@RequestBody Contact contact) {
        // 关键动作：如果有联系方式，先把它们的“爸爸”设为当前联系人
        // 这样存进数据库时，contact_id 就有值了，不会报 NOT NULL 错误
        if (contact.getDetails() != null) {
            for (ContactDetail detail : contact.getDetails()) {
                detail.setContact(contact);
            }
        }
        return contactRepository.save(contact);
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        contactRepository.deleteById(id);
    }

    @PostMapping("/favorite/{id}")
    public Contact toggleFavorite(@PathVariable Long id) {
        Contact contact = contactRepository.findById(id).orElseThrow();
        contact.setIsFavorite(contact.getIsFavorite() == null || !contact.getIsFavorite());
        return contactRepository.save(contact);
    }

    // 导出
    @GetMapping("/export")
    public void download(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("通讯录完整导出", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 1. 查出数据
        List<Contact> dbList = contactRepository.findAll();

        // 2. 准备导出列表
        List<ContactExportVO> exportList = new java.util.ArrayList<>();

        // 3. 定义一个计数器，从 1 开始 (为了让Excel里显示 1, 2, 3...)
        long index = 1;

        for (Contact contact : dbList) {
            ContactExportVO vo = new ContactExportVO();

            // ❌ 之前是 vo.setId(contact.getId()); --> 这是真实ID (1, 10)
            // ✅ 现在改成 vo.setId(index++); --> 这是序号 (1, 2)，每循环一次加1
            vo.setId(index++);

            vo.setName(contact.getName());
            vo.setAddress(contact.getAddress());
            vo.setIsFavorite(Boolean.TRUE.equals(contact.getIsFavorite()) ? "★" : "");

            // 处理联系方式拼接
            if (contact.getDetails() != null) {
                StringBuilder phoneSb = new StringBuilder();
                StringBuilder wechatSb = new StringBuilder();
                StringBuilder emailSb = new StringBuilder();
                StringBuilder otherSb = new StringBuilder();

                for (ContactDetail detail : contact.getDetails()) {
                    String type = detail.getType();
                    String content = detail.getContent();
                    if (type == null)
                        continue;

                    if (type.contains("手机")) {
                        phoneSb.append(content).append("; ");
                    } else if (type.contains("微信")) {
                        wechatSb.append(content).append("; ");
                    } else if (type.contains("邮箱")) {
                        emailSb.append(content).append("; ");
                    } else {
                        otherSb.append(type).append(":").append(content).append("; ");
                    }
                }
                vo.setPhone(phoneSb.toString());
                vo.setWechat(wechatSb.toString());
                vo.setEmail(emailSb.toString());
                vo.setOther(otherSb.toString());
            }
            exportList.add(vo);
        }

        // 4. 写出
        EasyExcel.write(response.getOutputStream(), ContactExportVO.class)
                .sheet("联系人详细表")
                .doWrite(exportList);
    }

    // 导入
    @PostMapping("/import")
    public String upload(@RequestParam("file") MultipartFile file) throws IOException {
        EasyExcel.read(file.getInputStream(), Contact.class, new ContactListener(contactRepository)).sheet().doRead();
        return "success";
    }
}