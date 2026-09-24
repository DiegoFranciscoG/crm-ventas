import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { IdentificationResult, TaxpayerType } from '../core/models';

/**
 * Validación de cédula y RUC en el navegador (misma lógica que el backend, que es la autoridad).
 * Sirve para dar retroalimentación inmediata en los formularios. Ver docs/investigacion.md (#3-#6, S1, S3).
 */
const CEDULA = [2, 1, 2, 1, 2, 1, 2, 1, 2];
const PRIVATE = [4, 3, 2, 7, 6, 5, 4, 3, 2];
const PUBLIC = [3, 2, 7, 6, 5, 4, 3, 2];

const invalid = (message: string): IdentificationResult => ({
  valid: false,
  taxpayerType: null,
  checksumVerified: false,
  message,
});

const valid = (taxpayerType: TaxpayerType | null, checksumVerified: boolean, message: string): IdentificationResult => ({
  valid: true,
  taxpayerType,
  checksumVerified,
  message,
});

const digits = (value: string, length: number) => new RegExp(`^[0-9]{${length}}$`).test(value);
const province = (value: string) => Number(value.slice(0, 2));
const domestic = (code: number) => code >= 1 && code <= 24;

function modulo11(value: string, coefficients: number[]): number {
  const sum = coefficients.reduce((acc, c, i) => acc + Number(value[i]) * c, 0);
  const remainder = sum % 11;
  if (remainder === 0) {
    return 0;
  }
  const check = 11 - remainder;
  return check === 10 ? -1 : check;
}

export function validateCedula(value: string | null | undefined): IdentificationResult {
  const cedula = value ?? '';
  if (!digits(cedula, 10)) {
    return invalid('La cédula debe tener exactamente 10 dígitos.');
  }
  const code = province(cedula);
  if (!domestic(code) && code !== 30) {
    return invalid('Código de provincia inválido: debe estar entre 01 y 24, o ser 30.');
  }
  if (Number(cedula[2]) > 5) {
    return invalid('El tercer dígito de una cédula debe estar entre 0 y 5.');
  }
  const sum = CEDULA.reduce((acc, c, i) => {
    const product = Number(cedula[i]) * c;
    return acc + (product >= 10 ? product - 9 : product);
  }, 0);
  if ((10 - (sum % 10)) % 10 !== Number(cedula[9])) {
    return invalid('El dígito verificador de la cédula no es correcto.');
  }
  return valid(null, true, 'Cédula válida.');
}

export function validateRuc(value: string | null | undefined): IdentificationResult {
  const ruc = value ?? '';
  if (!digits(ruc, 13)) {
    return invalid('El RUC debe tener exactamente 13 dígitos.');
  }
  const third = Number(ruc[2]);
  if (third <= 5) {
    const cedula = validateCedula(ruc.slice(0, 10));
    if (!cedula.valid) {
      return invalid(`RUC de persona natural con cédula inválida: ${cedula.message}`);
    }
    return ruc.endsWith('001')
      ? valid('NATURAL_PERSON', true, 'RUC de persona natural válido.')
      : invalid('El RUC de persona natural es la cédula seguida de 001.');
  }
  if (!domestic(province(ruc))) {
    return invalid('Código de provincia inválido: debe estar entre 01 y 24.');
  }
  if (third === 6) {
    if (ruc.endsWith('0000')) {
      return invalid('El establecimiento de un RUC público no puede ser 0000.');
    }
    return modulo11(ruc, PUBLIC) === Number(ruc[8])
      ? valid('PUBLIC_ENTITY', true, 'RUC de entidad pública válido.')
      : valid('PUBLIC_ENTITY', false, 'Estructura válida; confirme el RUC en la consulta pública del SRI.');
  }
  if (third === 9) {
    if (!ruc.endsWith('001')) {
      return invalid('El RUC de una sociedad termina siempre en 001.');
    }
    return modulo11(ruc, PRIVATE) === Number(ruc[9])
      ? valid('PRIVATE_COMPANY', true, 'RUC de sociedad válido.')
      : valid(
          'PRIVATE_COMPANY',
          false,
          'Estructura válida. El SRI no aplica módulo 11 a los RUC con secuencial de 7 dígitos; confírmelo en la consulta pública del SRI.',
        );
  }
  return invalid('El tercer dígito del RUC debe ser 0-5 (persona natural), 6 (pública) o 9 (sociedad).');
}

/** Validador de formulario: devuelve el mensaje del algoritmo como error. */
export function identificationValidator(kind: 'RUC' | 'CEDULA', optional = false): ValidatorFn {
  return (control: AbstractControl<string | null>): ValidationErrors | null => {
    const value = (control.value ?? '').trim();
    if (!value) {
      return optional ? null : { required: true };
    }
    const result = kind === 'RUC' ? validateRuc(value) : validateCedula(value);
    return result.valid ? null : { identification: result.message };
  };
}
