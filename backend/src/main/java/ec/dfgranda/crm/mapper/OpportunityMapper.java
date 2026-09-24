package ec.dfgranda.crm.mapper;

import ec.dfgranda.crm.domain.Opportunity;
import ec.dfgranda.crm.domain.OpportunityStageHistory;
import ec.dfgranda.crm.dto.OpportunityCard;
import ec.dfgranda.crm.dto.OpportunityResponse;
import ec.dfgranda.crm.dto.StageHistoryResponse;

import java.time.LocalDate;

public final class OpportunityMapper {

    private OpportunityMapper() {
    }

    public static OpportunityResponse toResponse(Opportunity o) {
        return new OpportunityResponse(o.getId(), o.getTitle(), o.getCompany().getId(), o.getCompany().getLegalName(),
                o.getPrimaryContact() == null ? null : o.getPrimaryContact().getId(),
                o.getPrimaryContact() == null ? null : o.getPrimaryContact().fullName(),
                UserMapper.toSummary(o.getOwner()), o.getStage().getCode(), o.getStage().getName(), o.getAmount(),
                o.getProbability(), o.weightedAmount(), o.getExpectedCloseDate(), o.getClosedAt(), o.getLostReason(),
                o.getDescription(), o.getVersion(), o.getCreatedAt(), o.getUpdatedAt());
    }

    public static OpportunityCard toCard(Opportunity o, LocalDate today) {
        boolean overdue = !o.getStage().isClosed() && o.getExpectedCloseDate().isBefore(today);
        return new OpportunityCard(o.getId(), o.getTitle(), o.getCompany().getId(), o.getCompany().getLegalName(),
                UserMapper.toSummary(o.getOwner()), o.getAmount(), o.getProbability(), o.getExpectedCloseDate(),
                overdue, o.getVersion());
    }

    public static StageHistoryResponse toResponse(OpportunityStageHistory history, String changedByName) {
        return new StageHistoryResponse(history.getFromStage(), history.getToStage(), history.getAmount(),
                history.getProbability(), changedByName, history.getChangedAt());
    }
}
