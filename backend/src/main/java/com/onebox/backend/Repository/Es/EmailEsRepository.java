package com.onebox.backend.Repository.Es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import com.onebox.backend.Model.Email;

public interface EmailEsRepository extends ElasticsearchRepository<Email, Long> {

    void deleteAllByAccountId(Long accountId);
    
}