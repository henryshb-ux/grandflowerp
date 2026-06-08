package com.artivisi.accountingfinance.service;

import com.artivisi.accountingfinance.entity.Client;
import com.artivisi.accountingfinance.entity.JournalEntry;
import com.artivisi.accountingfinance.entity.TaxTransactionDetail;
import com.artivisi.accountingfinance.entity.Transaction;
import com.artivisi.accountingfinance.enums.TaxType;
import com.artivisi.accountingfinance.enums.TemplateCategory;
import com.artivisi.accountingfinance.repository.TaxTransactionDetailRepository;
import com.artivisi.accountingfinance.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class TaxTransactionDetailService {

    private static final String DETAIL_NOT_FOUND = "Tax detail tidak ditemukan: ";
    private static final String TRANSACTION_NOT_FOUND = "Transaksi tidak ditemukan: ";

    // Tax account codes for auto-detection
    private static final String ACCOUNT_HUTANG_PPN = "2.1.03";
    private static final String ACCOUNT_PPN_MASUKAN = "1.1.25";
    private static final String ACCOUNT_KREDIT_PPH_23 = "1.1.26";
    private static final String ACCOUNT_HUTANG_PPH_21 = "2.1.20";
    private static final String ACCOUNT_HUTANG_PPH_23 = "2.1.21";
    private static final String ACCOUNT_HUTANG_PPH_42 = "2.1.22";

    private final TaxTransactionDetailRepository taxDetailRepository;
    private final TransactionRepository transactionRepository;
    private final TaxTransactionDetailService self;

    public TaxTransactionDetailService(
            TaxTransactionDetailRepository taxDetailRepository,
            TransactionRepository transactionRepository,
            @Lazy TaxTransactionDetailService self) {
        this.taxDetailRepository = taxDetailRepository;
        this.transactionRepository = transactionRepository;
        this.self = self;
    }

    public List<TaxTransactionDetail> findByTransactionId(UUID transactionId) {
        return taxDetailRepository.findAllByTransactionIdOrderByTaxTypeAsc(transactionId);
    }

    public TaxTransactionDetail findById(UUID id) {
        return taxDetailRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(DETAIL_NOT_FOUND + id));
    }

    @Transactional
    public TaxTransactionDetail save(UUID transactionId, TaxTransactionDetail detail) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException(TRANSACTION_NOT_FOUND + transactionId));

        validate(detail, null);

        detail.setTransaction(transaction);
        return taxDetailRepository.save(detail);
    }

    @Transactional
    public TaxTransactionDetail update(UUID detailId, TaxTransactionDetail updated) {
        TaxTransactionDetail existing = taxDetailRepository.findById(detailId)
                .orElseThrow(() -> new EntityNotFoundException(DETAIL_NOT_FOUND + detailId));

        validate(updated, detailId);

        // Merge fields
        existing.setTaxType(updated.getTaxType());
        existing.setFakturNumber(updated.getFakturNumber());
        existing.setFakturDate(updated.getFakturDate());
        existing.setTransactionCode(updated.getTransactionCode());
        existing.setDpp(updated.getDpp());
        existing.setPpn(updated.getPpn());
        existing.setPpnbm(updated.getPpnbm());
        existing.setBupotNumber(updated.getBupotNumber());
        existing.setTaxObjectCode(updated.getTaxObjectCode());
        existing.setGrossAmount(updated.getGrossAmount());
        existing.setTaxRate(updated.getTaxRate());
        existing.setTaxAmount(updated.getTaxAmount());
        existing.setCounterpartyNpwp(updated.getCounterpartyNpwp());
        existing.setCounterpartyNitku(updated.getCounterpartyNitku());
        existing.setCounterpartyNik(updated.getCounterpartyNik());
        existing.setCounterpartyIdType(updated.getCounterpartyIdType());
        existing.setCounterpartyName(updated.getCounterpartyName());
        existing.setCounterpartyAddress(updated.getCounterpartyAddress());

        return taxDetailRepository.save(existing);
    }

    @Transactional
    public void delete(UUID detailId) {
        TaxTransactionDetail detail = taxDetailRepository.findById(detailId)
                .orElseThrow(() -> new EntityNotFoundException(DETAIL_NOT_FOUND + detailId));
        taxDetailRepository.delete(detail);
    }

    @Transactional
    public int autoPopulateFromTransaction(Transaction transaction) {
        List<TaxTransactionDetail> existing = findByTransactionId(transaction.getId());
        if (!existing.isEmpty()) {
            log.debug("Transaction {} already has {} tax details, skipping auto-populate",
                    transaction.getId(), existing.size());
            return 0;
        }

        List<TaxDetailSuggestion> suggestions = suggestFromTransaction(transaction);
        if (suggestions.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (TaxDetailSuggestion suggestion : suggestions) {
            if (suggestion.name() == null || suggestion.name().isBlank()) {
                log.debug("Skipping auto-populate for {} — no counterparty name available",
                        suggestion.taxType());
                continue;
            }

            TaxTransactionDetail detail = new TaxTransactionDetail();
            detail.setTaxType(suggestion.taxType());
            detail.setTransactionCode(suggestion.transactionCode());
            detail.setDpp(suggestion.dpp());
            detail.setPpn(suggestion.ppn());
            detail.setGrossAmount(suggestion.grossAmount());
            detail.setTaxRate(suggestion.taxRate());
            detail.setTaxAmount(suggestion.taxAmount());
            detail.setCounterpartyNpwp(suggestion.npwp());
            detail.setCounterpartyNitku(suggestion.nitku());
            detail.setCounterpartyNik(suggestion.nik());
            detail.setCounterpartyIdType(suggestion.idType());
            detail.setCounterpartyName(suggestion.name());
            detail.setCounterpartyAddress(suggestion.address());

            self.save(transaction.getId(), detail);
            count++;
        }

        log.info("Auto-populated {} tax details for transaction {}", count, transaction.getId());
        return count;
    }

    public Set<UUID> findTransactionIdsWithDetails(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return taxDetailRepository.findTransactionIdsWithDetails(ids);
    }

    /**
     * Analyze a transaction's template name and journal entries to suggest tax detail values.
     */
    public List<TaxDetailSuggestion> suggestFromTransaction(Transaction transaction) {
        List<TaxDetailSuggestion> suggestions = new ArrayList<>();
        String templateName = transaction.getJournalTemplate() != null
                ? transaction.getJournalTemplate().getTemplateName() : "";
        String templateNameUpper = templateName.toUpperCase();
        TemplateCategory category = transaction.getJournalTemplate() != null
                ? transaction.getJournalTemplate().getCategory() : null;

        Client client = (transaction.getProject() != null) ? transaction.getProject().getClient() : null;

        suggestPpn(transaction, templateNameUpper, category, client, suggestions);
        suggestPph23(transaction, templateNameUpper, client, suggestions);
        suggestPph42(transaction, templateNameUpper, client, suggestions);
        suggestPph21(transaction, templateNameUpper, client, suggestions);

        return suggestions;
    }

    private void suggestPpn(Transaction transaction, String templateNameUpper,
                            TemplateCategory category, Client client, List<TaxDetailSuggestion> suggestions) {
        if (!templateNameUpper.contains("PPN")) {
            return;
        }

        TaxType ppnType = (category == TemplateCategory.INCOME || category == TemplateCategory.RECEIPT)
                ? TaxType.PPN_KELUARAN : TaxType.PPN_MASUKAN;

        BigDecimal ppn = findPpnAmountFromEntries(transaction.getJournalEntries());
        BigDecimal dpp = transaction.getAmount();

        String transactionCode = (templateNameUpper.contains("BUMN")
                || templateNameUpper.contains("FP 03")) ? "03" : "01";

        suggestions.add(buildSuggestion(ppnType, transactionCode,
                new TaxAmounts(dpp, ppn, null, null, null), client));
    }

    private BigDecimal findPpnAmountFromEntries(List<JournalEntry> entries) {
        for (JournalEntry entry : entries) {
            String accountCode = getAccountCode(entry);
            if (ACCOUNT_HUTANG_PPN.equals(accountCode)) {
                return entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO;
            } else if (ACCOUNT_PPN_MASUKAN.equals(accountCode)) {
                return entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private void suggestPph23(Transaction transaction, String templateNameUpper,
                              Client client, List<TaxDetailSuggestion> suggestions) {
        if (!templateNameUpper.contains("PPH 23") && !templateNameUpper.contains("PPH23")) {
            return;
        }

        BigDecimal taxAmount = findTaxAmountFromEntries(transaction.getJournalEntries(),
                ACCOUNT_KREDIT_PPH_23, ACCOUNT_HUTANG_PPH_23);

        suggestions.add(buildSuggestion(TaxType.PPH_23, null,
                new TaxAmounts(null, null, transaction.getAmount(), new BigDecimal("2.00"), taxAmount), client));
    }

    private void suggestPph42(Transaction transaction, String templateNameUpper,
                              Client client, List<TaxDetailSuggestion> suggestions) {
        if (!templateNameUpper.contains("PPH 4(2)") && !templateNameUpper.contains("PPH 42")
                && !templateNameUpper.contains("PPH4(2)")) {
            return;
        }

        BigDecimal taxAmount = BigDecimal.ZERO;
        for (JournalEntry entry : transaction.getJournalEntries()) {
            if (ACCOUNT_HUTANG_PPH_42.equals(getAccountCode(entry))) {
                taxAmount = entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO;
            }
        }

        suggestions.add(buildSuggestion(TaxType.PPH_42, null,
                new TaxAmounts(null, null, transaction.getAmount(), new BigDecimal("10.00"), taxAmount), client));
    }

    private void suggestPph21(Transaction transaction, String templateNameUpper,
                              Client client, List<TaxDetailSuggestion> suggestions) {
        if (!templateNameUpper.contains("PPH 21") && !templateNameUpper.contains("PPH21")
                && !templateNameUpper.contains("GAJI")) {
            return;
        }

        BigDecimal taxAmount = BigDecimal.ZERO;
        for (JournalEntry entry : transaction.getJournalEntries()) {
            if (ACCOUNT_HUTANG_PPH_21.equals(getAccountCode(entry))) {
                taxAmount = entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO;
            }
        }
        if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
            suggestions.add(buildSuggestion(TaxType.PPH_21, null,
                    new TaxAmounts(null, null, transaction.getAmount(), null, taxAmount), client));
        }
    }

    private BigDecimal findTaxAmountFromEntries(List<JournalEntry> entries,
                                                 String debitAccount, String creditAccount) {
        for (JournalEntry entry : entries) {
            String accountCode = getAccountCode(entry);
            if (debitAccount.equals(accountCode)) {
                return entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO;
            } else if (creditAccount.equals(accountCode)) {
                return entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private String getAccountCode(JournalEntry entry) {
        return entry.getAccount() != null ? entry.getAccount().getAccountCode() : "";
    }

    private TaxDetailSuggestion buildSuggestion(TaxType taxType, String transactionCode,
                                                 TaxAmounts amounts, Client client) {
        return new TaxDetailSuggestion(
                taxType, transactionCode,
                amounts.dpp(), amounts.ppn(), amounts.grossAmount(),
                amounts.taxRate(), amounts.taxAmount(),
                client != null ? client.getNpwp() : null,
                client != null ? client.getNitku() : null,
                client != null ? client.getNik() : null,
                client != null ? client.getIdType() : "TIN",
                client != null ? client.getName() : null,
                client != null ? client.getAddress() : null
        );
    }

    private void validate(TaxTransactionDetail detail, UUID existingId) {
        validateRequiredFields(detail);
        validateEFaktur(detail, existingId);
        validateEBupot(detail, existingId);
        validateNpwpFormat(detail);
    }

    private void validateRequiredFields(TaxTransactionDetail detail) {
        if (detail.getTaxType() == null) {
            throw new IllegalArgumentException("Jenis pajak (taxType) wajib diisi");
        }
        if (detail.getCounterpartyName() == null || detail.getCounterpartyName().isBlank()) {
            throw new IllegalArgumentException("Nama lawan transaksi wajib diisi");
        }
    }

    private void validateEFaktur(TaxTransactionDetail detail, UUID existingId) {
        if (!detail.isEFaktur()) {
            return;
        }
        if (detail.getDpp() == null) {
            throw new IllegalArgumentException("DPP wajib diisi untuk e-Faktur");
        }
        if (detail.getPpn() == null) {
            throw new IllegalArgumentException("PPN wajib diisi untuk e-Faktur");
        }
        if (detail.getFakturNumber() != null && !detail.getFakturNumber().isBlank()) {
            boolean duplicate = (existingId == null)
                    ? taxDetailRepository.existsByFakturNumber(detail.getFakturNumber())
                    : taxDetailRepository.existsByFakturNumberAndIdNot(detail.getFakturNumber(), existingId);
            if (duplicate) {
                throw new IllegalArgumentException("Nomor faktur sudah digunakan: " + detail.getFakturNumber());
            }
        }
    }

    private void validateEBupot(TaxTransactionDetail detail, UUID existingId) {
        if (!detail.isEBupot()) {
            return;
        }
        if (detail.getGrossAmount() == null) {
            throw new IllegalArgumentException("Jumlah bruto wajib diisi untuk e-Bupot");
        }
        if (detail.getTaxRate() == null) {
            throw new IllegalArgumentException("Tarif pajak wajib diisi untuk e-Bupot");
        }
        if (detail.getTaxAmount() == null) {
            throw new IllegalArgumentException("Jumlah pajak wajib diisi untuk e-Bupot");
        }
        if (detail.getBupotNumber() != null && !detail.getBupotNumber().isBlank()) {
            boolean duplicate = (existingId == null)
                    ? taxDetailRepository.existsByBupotNumber(detail.getBupotNumber())
                    : taxDetailRepository.existsByBupotNumberAndIdNot(detail.getBupotNumber(), existingId);
            if (duplicate) {
                throw new IllegalArgumentException("Nomor bukti potong sudah digunakan: " + detail.getBupotNumber());
            }
        }
    }

    private void validateNpwpFormat(TaxTransactionDetail detail) {
        if (detail.getCounterpartyNpwp() == null || detail.getCounterpartyNpwp().isBlank()) {
            return;
        }
        String npwp = detail.getCounterpartyNpwp().replaceAll("[^0-9]", "");
        if (npwp.length() != 15 && npwp.length() != 16) {
            throw new IllegalArgumentException("NPWP harus 15 atau 16 digit");
        }
    }

    public record TaxAmounts(
            BigDecimal dpp, BigDecimal ppn,
            BigDecimal grossAmount, BigDecimal taxRate, BigDecimal taxAmount
    ) {}

    public record TaxDetailSuggestion(
            TaxType taxType, String transactionCode,
            BigDecimal dpp, BigDecimal ppn,
            BigDecimal grossAmount, BigDecimal taxRate, BigDecimal taxAmount,
            String npwp, String nitku, String nik, String idType,
            String name, String address
    ) {}
}
