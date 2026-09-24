import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { APP_CONFIG } from '../config';
import {
  Activity,
  ActivityRequest,
  AuditLogEntry,
  BoardColumn,
  Catalogs,
  Company,
  CompanyRequest,
  CompanySummary,
  Consent,
  ConsentRequest,
  Contact,
  ContactCreateRequest,
  ContactSummary,
  ContactUpdateRequest,
  DataSubjectRequest,
  ErasureResult,
  Forecast,
  IdentificationResult,
  Opportunity,
  Page,
  Product,
  Quote,
  QuoteRequest,
  QuoteStatus,
  QuoteSummary,
  RequestChannel,
  Role,
  StageHistory,
  User,
} from '../models';

type Params = Record<string, string | number | boolean | null | undefined>;

/** Cliente tipado del API REST. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(APP_CONFIG).apiUrl}/api`;
  private catalogs$?: Observable<Catalogs>;

  // ---- Catálogos y validación
  catalogs(): Observable<Catalogs> {
    this.catalogs$ ??= this.http.get<Catalogs>(`${this.base}/catalogs`).pipe(shareReplay(1));
    return this.catalogs$;
  }

  validateIdentification(type: 'RUC' | 'CEDULA', number: string): Observable<IdentificationResult> {
    return this.http.post<IdentificationResult>(`${this.base}/validation/identification`, { type, number });
  }

  // ---- Usuarios
  users(): Observable<User[]> {
    return this.http.get<User[]>(`${this.base}/users`);
  }

  createUser(body: { email: string; fullName: string; password: string; role: Role }): Observable<User> {
    return this.http.post<User>(`${this.base}/users`, body);
  }

  updateUser(id: number, body: { fullName: string; role: Role; active: boolean; newPassword?: string | null }): Observable<User> {
    return this.http.put<User>(`${this.base}/users/${id}`, body);
  }

  // ---- Empresas
  companies(params: Params): Observable<Page<CompanySummary>> {
    return this.http.get<Page<CompanySummary>>(`${this.base}/companies`, { params: toParams(params) });
  }

  company(id: number): Observable<Company> {
    return this.http.get<Company>(`${this.base}/companies/${id}`);
  }

  createCompany(body: CompanyRequest): Observable<Company> {
    return this.http.post<Company>(`${this.base}/companies`, body);
  }

  updateCompany(id: number, body: CompanyRequest): Observable<Company> {
    return this.http.put<Company>(`${this.base}/companies/${id}`, body);
  }

  deleteCompany(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/companies/${id}`);
  }

  // ---- Contactos y consentimientos
  contacts(params: Params): Observable<Page<ContactSummary>> {
    return this.http.get<Page<ContactSummary>>(`${this.base}/contacts`, { params: toParams(params) });
  }

  contact(id: number): Observable<Contact> {
    return this.http.get<Contact>(`${this.base}/contacts/${id}`);
  }

  createContact(body: ContactCreateRequest): Observable<Contact> {
    return this.http.post<Contact>(`${this.base}/contacts`, body);
  }

  updateContact(id: number, body: ContactUpdateRequest): Observable<Contact> {
    return this.http.put<Contact>(`${this.base}/contacts/${id}`, body);
  }

  grantConsent(contactId: number, body: ConsentRequest): Observable<Consent> {
    return this.http.post<Consent>(`${this.base}/contacts/${contactId}/consents`, body);
  }

  revokeConsent(contactId: number, consentId: number, reason: string): Observable<Consent> {
    return this.http.post<Consent>(`${this.base}/contacts/${contactId}/consents/${consentId}/revoke`, { reason });
  }

  exportContact(contactId: number, channel: RequestChannel, notes: string): Observable<Blob> {
    return this.http.post(`${this.base}/contacts/${contactId}/export`, { channel, notes }, { responseType: 'blob' });
  }

  eraseContact(contactId: number, channel: RequestChannel, notes: string): Observable<ErasureResult> {
    return this.http.post<ErasureResult>(`${this.base}/contacts/${contactId}/erasure`, { channel, notes });
  }

  // ---- Oportunidades
  board(ownerId?: number | null): Observable<BoardColumn[]> {
    return this.http.get<BoardColumn[]>(`${this.base}/opportunities/board`, { params: toParams({ ownerId }) });
  }

  opportunities(params: Params): Observable<Page<Opportunity>> {
    return this.http.get<Page<Opportunity>>(`${this.base}/opportunities`, { params: toParams(params) });
  }

  opportunity(id: number): Observable<Opportunity> {
    return this.http.get<Opportunity>(`${this.base}/opportunities/${id}`);
  }

  createOpportunity(body: {
    title: string;
    companyId: number;
    primaryContactId?: number | null;
    amount: number;
    expectedCloseDate: string;
    stageCode?: string | null;
    description?: string | null;
    ownerId?: number | null;
  }): Observable<Opportunity> {
    return this.http.post<Opportunity>(`${this.base}/opportunities`, body);
  }

  updateOpportunity(
    id: number,
    body: {
      title: string;
      primaryContactId?: number | null;
      amount: number;
      expectedCloseDate: string;
      probability?: number | null;
      description?: string | null;
      ownerId?: number | null;
      version: number;
    },
  ): Observable<Opportunity> {
    return this.http.put<Opportunity>(`${this.base}/opportunities/${id}`, body);
  }

  changeStage(id: number, stageCode: string, version: number, lostReason?: string | null): Observable<Opportunity> {
    return this.http.patch<Opportunity>(`${this.base}/opportunities/${id}/stage`, { stageCode, version, lostReason });
  }

  deleteOpportunity(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/opportunities/${id}`);
  }

  stageHistory(id: number): Observable<StageHistory[]> {
    return this.http.get<StageHistory[]>(`${this.base}/opportunities/${id}/history`);
  }

  // ---- Actividades
  activities(params: Params): Observable<Page<Activity>> {
    return this.http.get<Page<Activity>>(`${this.base}/activities`, { params: toParams(params) });
  }

  createActivity(body: ActivityRequest): Observable<Activity> {
    return this.http.post<Activity>(`${this.base}/activities`, body);
  }

  completeActivity(id: number, completed: boolean): Observable<Activity> {
    return this.http.post<Activity>(`${this.base}/activities/${id}/${completed ? 'complete' : 'reopen'}`, {});
  }

  deleteActivity(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/activities/${id}`);
  }

  // ---- Productos
  products(includeInactive = false): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.base}/products`, { params: toParams({ includeInactive }) });
  }

  saveProduct(
    id: number | null,
    body: { sku: string; name: string; description?: string | null; unitPrice: number; vatRateCode: string; active: boolean },
  ): Observable<Product> {
    return id
      ? this.http.put<Product>(`${this.base}/products/${id}`, body)
      : this.http.post<Product>(`${this.base}/products`, body);
  }

  // ---- Cotizaciones
  quotes(params: Params): Observable<Page<QuoteSummary>> {
    return this.http.get<Page<QuoteSummary>>(`${this.base}/quotes`, { params: toParams(params) });
  }

  quote(id: number): Observable<Quote> {
    return this.http.get<Quote>(`${this.base}/quotes/${id}`);
  }

  saveQuote(id: number | null, body: QuoteRequest): Observable<Quote> {
    return id ? this.http.put<Quote>(`${this.base}/quotes/${id}`, body) : this.http.post<Quote>(`${this.base}/quotes`, body);
  }

  changeQuoteStatus(id: number, status: QuoteStatus): Observable<Quote> {
    return this.http.patch<Quote>(`${this.base}/quotes/${id}/status`, { status });
  }

  quotePdf(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/quotes/${id}/pdf`, { responseType: 'blob' });
  }

  // ---- Forecast y privacidad
  forecast(from: string, to: string, ownerId?: number | null): Observable<Forecast> {
    return this.http.get<Forecast>(`${this.base}/forecast`, { params: toParams({ from, to, ownerId }) });
  }

  dataSubjectRequests(page = 0, size = 20): Observable<Page<DataSubjectRequest>> {
    return this.http.get<Page<DataSubjectRequest>>(`${this.base}/privacy/requests`, { params: toParams({ page, size }) });
  }

  accessLog(params: Params): Observable<Page<AuditLogEntry>> {
    return this.http.get<Page<AuditLogEntry>>(`${this.base}/privacy/access-log`, { params: toParams(params) });
  }
}

function toParams(params: Params): HttpParams {
  let result = new HttpParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== null && value !== undefined && value !== '') {
      result = result.set(key, String(value));
    }
  }
  return result;
}

/** Descarga un Blob con un nombre de archivo sin abrir ventanas nuevas. */
export function saveBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = fileName;
  anchor.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
