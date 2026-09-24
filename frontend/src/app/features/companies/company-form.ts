import { FormBuilder, Validators } from '@angular/forms';
import { Company, CompanyRequest, IdentificationResult } from '../../core/models';
import { identificationValidator, validateRuc } from '../../shared/identification';

export function buildCompanyForm(fb: FormBuilder) {
  return fb.nonNullable.group({
    ruc: ['', [identificationValidator('RUC')]],
    legalName: ['', [Validators.required, Validators.maxLength(300)]],
    tradeName: [''],
    industryCode: [''],
    provinceCode: ['', Validators.required],
    city: [''],
    address: [''],
    phone: ['', Validators.pattern(/^[0-9+() -]{7,20}$/)],
    email: ['', Validators.email],
    website: ['', Validators.pattern(/^https:\/\/\S{3,190}$/)],
  });
}

export type CompanyForm = ReturnType<typeof buildCompanyForm>;

export function companyFormValue(form: CompanyForm): CompanyRequest {
  const v = form.getRawValue();
  const blank = (s: string) => (s.trim() ? s.trim() : null);
  return {
    ruc: v.ruc.trim(),
    legalName: v.legalName.trim(),
    tradeName: blank(v.tradeName),
    industryCode: blank(v.industryCode),
    provinceCode: v.provinceCode,
    city: blank(v.city),
    address: blank(v.address),
    phone: blank(v.phone),
    email: blank(v.email),
    website: blank(v.website),
  };
}

export function patchCompanyForm(form: CompanyForm, c: Company): void {
  form.reset({
    ruc: c.ruc,
    legalName: c.legalName,
    tradeName: c.tradeName ?? '',
    industryCode: c.industryCode ?? '',
    provinceCode: c.provinceCode,
    city: c.city ?? '',
    address: c.address ?? '',
    phone: c.phone ?? '',
    email: c.email ?? '',
    website: c.website ?? '',
  });
}

/** Resultado del algoritmo para mostrar "verificado" o la advertencia del SRI mientras se escribe. */
export function rucFeedback(ruc: string): IdentificationResult | null {
  const result = validateRuc(ruc.trim());
  return result.valid ? result : null;
}
