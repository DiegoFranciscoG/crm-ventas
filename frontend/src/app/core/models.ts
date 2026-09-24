// Contratos de la API (espejo de los DTO del backend).

export type Role = 'ADMIN' | 'MANAGER' | 'SALES_REP';
export type TaxpayerType = 'NATURAL_PERSON' | 'PUBLIC_ENTITY' | 'PRIVATE_COMPANY';
export type ForecastCategory = 'PIPELINE' | 'BEST_CASE' | 'COMMIT' | 'CLOSED' | 'OMITTED';
export type LegalBasis =
  | 'CONSENT'
  | 'LEGAL_OBLIGATION'
  | 'COURT_ORDER'
  | 'PUBLIC_INTEREST'
  | 'CONTRACT'
  | 'VITAL_INTEREST'
  | 'PUBLIC_DATABASE'
  | 'LEGITIMATE_INTEREST';
export type ConsentChannel = 'WEB_FORM' | 'EMAIL' | 'IN_PERSON' | 'PHONE_RECORDED' | 'PAPER';
export type ContactSource = 'DIRECT' | 'REFERRAL' | 'EVENT' | 'PUBLIC_SOURCE';
export type ActivityType = 'CALL' | 'MEETING' | 'EMAIL' | 'TASK' | 'NOTE';
export type QuoteStatus = 'DRAFT' | 'SENT' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED';
export type RequestChannel = 'EMAIL' | 'WEB_FORM' | 'IN_PERSON' | 'PHONE' | 'LETTER';
export type RequestType = 'ACCESS' | 'RECTIFICATION' | 'ERASURE' | 'OPPOSITION' | 'PORTABILITY' | 'SUSPENSION';
export type AuditAction =
  | 'VIEW'
  | 'LIST'
  | 'CREATE'
  | 'UPDATE'
  | 'EXPORT'
  | 'ERASE'
  | 'CONSENT_GRANT'
  | 'CONSENT_REVOKE'
  | 'PDF_DOWNLOAD';
export type AuditResourceType = 'CONTACT' | 'COMPANY' | 'QUOTE' | 'DATA_SUBJECT_REQUEST';

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UserSummary {
  id: number;
  fullName: string;
}

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  active: boolean;
  lastLoginAt: string | null;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}

export interface CodeName {
  code: string;
  name: string;
}

export interface VatRate {
  code: string;
  description: string;
  percentage: number;
}

export interface Stage {
  code: string;
  name: string;
  position: number;
  defaultProbability: number;
  forecastCategory: ForecastCategory;
  closed: boolean;
  won: boolean;
}

export interface Purpose {
  code: string;
  name: string;
  description: string;
  legalBasis: LegalBasis;
  retentionMonths: number;
}

export interface Catalogs {
  provinces: CodeName[];
  industries: CodeName[];
  vatRates: VatRate[];
  stages: Stage[];
  purposes: Purpose[];
  roles: CodeName[];
  privacyNoticeVersion: string;
}

export interface IdentificationResult {
  valid: boolean;
  taxpayerType: TaxpayerType | null;
  checksumVerified: boolean;
  message: string;
}

export interface CompanySummary {
  id: number;
  ruc: string;
  taxpayerType: TaxpayerType;
  legalName: string;
  tradeName: string | null;
  city: string | null;
  provinceName: string;
  owner: UserSummary;
}

export interface Company {
  id: number;
  ruc: string;
  taxpayerType: TaxpayerType;
  rucChecksumVerified: boolean;
  legalName: string;
  tradeName: string | null;
  industryCode: string | null;
  industryName: string | null;
  provinceCode: string;
  provinceName: string;
  city: string | null;
  address: string | null;
  phone: string | null;
  email: string | null;
  website: string | null;
  owner: UserSummary;
  createdAt: string;
  updatedAt: string;
}

export interface CompanyRequest {
  ruc: string;
  legalName: string;
  tradeName?: string | null;
  industryCode?: string | null;
  provinceCode: string;
  city?: string | null;
  address?: string | null;
  phone?: string | null;
  email?: string | null;
  website?: string | null;
  ownerId?: number | null;
}

export interface Consent {
  id: number;
  purposeCode: string;
  purposeName: string;
  legalBasis: LegalBasis;
  channel: ConsentChannel;
  evidence: string;
  privacyNoticeVersion: string;
  grantedAt: string;
  expiresAt: string;
  revokedAt: string | null;
  revocationReason: string | null;
  active: boolean;
  recordedBy: string;
}

export interface ConsentRequest {
  purposeCode: string;
  channel: ConsentChannel;
  evidence: string;
}

export interface ContactSummary {
  id: number;
  companyId: number;
  companyName: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  jobTitle: string | null;
  commercialContactAllowed: boolean;
}

export interface Contact {
  id: number;
  companyId: number;
  companyName: string;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
  jobTitle: string | null;
  nationalIdMasked: string | null;
  dataSource: ContactSource;
  consents: Consent[];
  createdAt: string;
  updatedAt: string;
}

export interface ContactCreateRequest {
  companyId: number;
  firstName: string;
  lastName: string;
  email?: string | null;
  phone?: string | null;
  jobTitle?: string | null;
  nationalId?: string | null;
  dataSource: ContactSource;
  initialConsent: ConsentRequest;
}

export type ContactUpdateRequest = Omit<ContactCreateRequest, 'companyId' | 'initialConsent'>;

export interface Opportunity {
  id: number;
  title: string;
  companyId: number;
  companyName: string;
  primaryContactId: number | null;
  primaryContactName: string | null;
  owner: UserSummary;
  stageCode: string;
  stageName: string;
  amount: number;
  probability: number;
  weightedAmount: number;
  expectedCloseDate: string;
  closedAt: string | null;
  lostReason: string | null;
  description: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface OpportunityCard {
  id: number;
  title: string;
  companyId: number;
  companyName: string;
  owner: UserSummary;
  amount: number;
  probability: number;
  expectedCloseDate: string;
  overdue: boolean;
  version: number;
}

export interface BoardColumn {
  stage: Stage;
  count: number;
  totalAmount: number;
  weightedAmount: number;
  opportunities: OpportunityCard[];
}

export interface StageHistory {
  fromStage: string | null;
  toStage: string;
  amount: number;
  probability: number;
  changedBy: string;
  changedAt: string;
}

export interface Activity {
  id: number;
  type: ActivityType;
  subject: string;
  notes: string | null;
  dueAt: string | null;
  remindAt: string | null;
  reminderSentAt: string | null;
  completedAt: string | null;
  owner: UserSummary;
  companyId: number | null;
  companyName: string | null;
  contactId: number | null;
  contactName: string | null;
  opportunityId: number | null;
  opportunityTitle: string | null;
  overdue: boolean;
}

export interface ActivityRequest {
  type: ActivityType;
  subject: string;
  notes?: string | null;
  dueAt?: string | null;
  remindAt?: string | null;
  companyId?: number | null;
  contactId?: number | null;
  opportunityId?: number | null;
}

export interface Product {
  id: number;
  sku: string;
  name: string;
  description: string | null;
  unitPrice: number;
  vatRateCode: string;
  vatPercentage: number;
  active: boolean;
}

export interface QuoteItem {
  lineNumber: number;
  productId: number | null;
  sku: string | null;
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  vatRateCode: string;
  vatPercentage: number;
  lineSubtotal: number;
  lineDiscount: number;
  lineTax: number;
  lineTotal: number;
}

export interface TaxLine {
  vatRateCode: string;
  percentage: number;
  taxableBase: number;
  tax: number;
}

export interface Quote {
  id: number;
  number: string;
  opportunityId: number;
  opportunityTitle: string;
  companyId: number;
  companyName: string;
  contactId: number | null;
  contactName: string | null;
  status: QuoteStatus;
  issueDate: string;
  validUntil: string;
  currency: string;
  subtotal: number;
  discountTotal: number;
  taxTotal: number;
  total: number;
  taxBreakdown: TaxLine[];
  notes: string | null;
  items: QuoteItem[];
  createdBy: string;
  createdAt: string;
}

export interface QuoteSummary {
  id: number;
  number: string;
  opportunityId: number;
  opportunityTitle: string;
  companyName: string;
  status: QuoteStatus;
  issueDate: string;
  validUntil: string;
  total: number;
}

export interface QuoteItemRequest {
  productId?: number | null;
  description?: string | null;
  quantity: number;
  unitPrice?: number | null;
  discountPercent?: number | null;
  vatRateCode?: string | null;
}

export interface QuoteRequest {
  opportunityId: number;
  contactId?: number | null;
  issueDate?: string | null;
  validUntil?: string | null;
  notes?: string | null;
  items: QuoteItemRequest[];
}

export interface ForecastTotals {
  openPipeline: number;
  weightedPipeline: number;
  commit: number;
  bestCase: number;
  closedWon: number;
  closedLost: number;
  winRate: number;
  openCount: number;
  wonCount: number;
  lostCount: number;
}

export interface Forecast {
  from: string;
  to: string;
  ownerId: number | null;
  totals: ForecastTotals;
  byMonth: {
    month: string;
    pipeline: number;
    weighted: number;
    commit: number;
    bestCase: number;
    closedWon: number;
  }[];
  byStage: { stageCode: string; stageName: string; count: number; amount: number; weighted: number }[];
  byOwner: { ownerId: number; ownerName: string; openCount: number; weighted: number; closedWon: number }[];
}

export interface DataSubjectRequest {
  id: number;
  contactId: number;
  requestType: RequestType;
  channel: RequestChannel;
  receivedAt: string;
  dueAt: string;
  status: 'RECEIVED' | 'COMPLETED' | 'REJECTED';
  completedAt: string | null;
  resolution: string | null;
  handledBy: string;
}

export interface AuditLogEntry {
  id: number;
  occurredAt: string;
  userId: number;
  userName: string | null;
  action: AuditAction;
  resourceType: AuditResourceType;
  resourceId: number | null;
  success: boolean;
  ipAddress: string | null;
  detail: string | null;
}

export interface ErasureResult {
  request: DataSubjectRequest;
  activitiesAnonymized: number;
  quotesDetached: number;
  opportunitiesDetached: number;
  consentsDeleted: number;
}

export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}
