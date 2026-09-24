package ec.dfgranda.crm.dto;

/** Resultado de la eliminación: registro de la solicitud y cantidad de registros desvinculados. */
public record ErasureResponse(DataSubjectRequestResponse request, int activitiesAnonymized, int quotesDetached,
                              int opportunitiesDetached, int consentsDeleted) {
}
