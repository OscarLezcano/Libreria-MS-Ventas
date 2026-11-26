package com.bigobooks.repositories;

import java.util.List;

import com.bigobooks.entities.mail.MailOutbox;
import com.bigobooks.repository.BaseRepository;

public interface MailOutboxRepository extends BaseRepository<MailOutbox> {

	List<MailOutbox> findTop50ByPendingTrueOrderByCreatedAtAsc();
}
