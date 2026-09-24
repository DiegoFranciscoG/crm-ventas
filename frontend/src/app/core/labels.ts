import {
  ActivityType,
  AuditAction,
  ConsentChannel,
  ContactSource,
  LegalBasis,
  QuoteStatus,
  RequestChannel,
  RequestType,
  Role,
  TaxpayerType,
} from './models';

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Administrador',
  MANAGER: 'Gerente comercial',
  SALES_REP: 'Vendedor',
};

export const TAXPAYER_LABELS: Record<TaxpayerType, string> = {
  NATURAL_PERSON: 'Persona natural',
  PUBLIC_ENTITY: 'Entidad pública',
  PRIVATE_COMPANY: 'Sociedad',
};

export const LEGAL_BASIS_LABELS: Record<LegalBasis, string> = {
  CONSENT: 'Consentimiento (Art. 7.1)',
  LEGAL_OBLIGATION: 'Obligación legal (Art. 7.2)',
  COURT_ORDER: 'Orden judicial (Art. 7.3)',
  PUBLIC_INTEREST: 'Interés público (Art. 7.4)',
  CONTRACT: 'Medidas precontractuales o contrato (Art. 7.5)',
  VITAL_INTEREST: 'Interés vital (Art. 7.6)',
  PUBLIC_DATABASE: 'Base de acceso público (Art. 7.7)',
  LEGITIMATE_INTEREST: 'Interés legítimo (Art. 7.8)',
};

export const CHANNEL_LABELS: Record<ConsentChannel, string> = {
  WEB_FORM: 'Formulario web',
  EMAIL: 'Correo electrónico',
  IN_PERSON: 'En persona (firmado)',
  PHONE_RECORDED: 'Llamada grabada',
  PAPER: 'Documento en papel',
};

export const SOURCE_LABELS: Record<ContactSource, string> = {
  DIRECT: 'Directamente del titular',
  REFERRAL: 'Referido',
  EVENT: 'Evento o feria',
  PUBLIC_SOURCE: 'Fuente de acceso público',
};

export const ACTIVITY_LABELS: Record<ActivityType, string> = {
  CALL: 'Llamada',
  MEETING: 'Reunión',
  EMAIL: 'Correo',
  TASK: 'Tarea',
  NOTE: 'Nota',
};

export const QUOTE_STATUS_LABELS: Record<QuoteStatus, string> = {
  DRAFT: 'Borrador',
  SENT: 'Enviada',
  ACCEPTED: 'Aceptada',
  REJECTED: 'Rechazada',
  EXPIRED: 'Vencida',
};

export const REQUEST_CHANNEL_LABELS: Record<RequestChannel, string> = {
  EMAIL: 'Correo electrónico',
  WEB_FORM: 'Formulario web',
  IN_PERSON: 'En persona',
  PHONE: 'Teléfono',
  LETTER: 'Carta',
};

export const REQUEST_TYPE_LABELS: Record<RequestType, string> = {
  ACCESS: 'Acceso',
  RECTIFICATION: 'Rectificación',
  ERASURE: 'Eliminación',
  OPPOSITION: 'Oposición',
  PORTABILITY: 'Acceso y portabilidad',
  SUSPENSION: 'Suspensión',
};

export const AUDIT_ACTION_LABELS: Record<AuditAction, string> = {
  VIEW: 'Consulta',
  LIST: 'Listado',
  CREATE: 'Creación',
  UPDATE: 'Actualización',
  EXPORT: 'Exportación',
  ERASE: 'Eliminación',
  CONSENT_GRANT: 'Consentimiento otorgado',
  CONSENT_REVOKE: 'Consentimiento revocado',
  PDF_DOWNLOAD: 'Descarga de PDF',
};

export function entries<K extends string>(labels: Record<K, string>): { value: K; label: string }[] {
  return (Object.keys(labels) as K[]).map((value) => ({ value, label: labels[value] }));
}
