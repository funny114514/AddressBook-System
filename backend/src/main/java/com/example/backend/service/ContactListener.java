package com.example.backend.service;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.example.backend.entity.Contact;
import com.example.backend.repository.ContactRepository;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ContactListener implements ReadListener<Contact> {

    private static final int BATCH_COUNT = 100; // 每100条存一次数据库
    private List<Contact> cachedDataList = new ArrayList<>(BATCH_COUNT);
    private ContactRepository contactRepository;

    // 构造函数传 Repository 进来，因为 Listener 没法自动注入
    public ContactListener(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Override
    public void invoke(Contact data, AnalysisContext context) {
        cachedDataList.add(data);
        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData(); // 处理剩余的数据
        log.info("Excel导入完成！");
    }

    private void saveData() {
        if (!cachedDataList.isEmpty()) {
            contactRepository.saveAll(cachedDataList); // JPA 的批量保存
            log.info("已保存 {} 条数据", cachedDataList.size());
            cachedDataList.clear();
        }
    }
}