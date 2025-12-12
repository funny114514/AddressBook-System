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

  
    @PostMapping("/save")
    public Contact save(@RequestBody Contact contact) {
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
        List<Contact> dbList = contactRepository.findAll();
        List<ContactExportVO> exportList = new java.util.ArrayList<>();
        long index = 1;

        for (Contact contact : dbList) {
            ContactExportVO vo = new ContactExportVO();
            vo.setId(index++);

            vo.setName(contact.getName());
            vo.setAddress(contact.getAddress());
            vo.setIsFavorite(Boolean.TRUE.equals(contact.getIsFavorite()) ? "★" : "");
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
