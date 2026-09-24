package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.PipelineStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, String> {

    List<PipelineStage> findAllByOrderByPositionAsc();
}
