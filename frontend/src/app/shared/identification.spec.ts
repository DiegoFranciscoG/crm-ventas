import { FormControl } from '@angular/forms';
import { identificationValidator, validateCedula, validateRuc } from './identification';

// Mismos casos que EcuadorianIdValidatorTest (backend): números sintéticos, nunca identificaciones reales.
describe('validateCedula', () => {
  it('acepta una cédula con verificador módulo 10 correcto', () => {
    expect(validateCedula('2059999991').valid).toBe(true);
    expect(validateCedula('3059999981').valid).toBe(true);
    expect(validateCedula('2459999971').valid).toBe(true);
  });

  it('rechaza verificador, provincia y tercer dígito inválidos', () => {
    expect(validateCedula('2059999992').message).toContain('verificador');
    expect(validateCedula('2559999991').message).toContain('provincia');
    expect(validateCedula('9059999991').message).toContain('provincia');
    expect(validateCedula('2069999991').message).toContain('tercer dígito');
  });

  it('rechaza formatos incorrectos', () => {
    for (const value of ['', '205999999', '20599999911', '20599999a1', null, undefined]) {
      expect(validateCedula(value).valid).toBe(false);
    }
  });
});

describe('validateRuc', () => {
  it('valida RUC de persona natural como cédula + 001', () => {
    expect(validateRuc('2059999991001')).toMatchObject({ valid: true, taxpayerType: 'NATURAL_PERSON' });
    expect(validateRuc('2059999991002').valid).toBe(false);
    expect(validateRuc('2059999992001').valid).toBe(false);
  });

  it('reproduce el ejemplo oficial del SRI (verificador 3)', () => {
    expect(validateRuc('1790085783001')).toMatchObject({ valid: true, checksumVerified: true, taxpayerType: 'PRIVATE_COMPANY' });
  });

  it('acepta sociedades con secuencial de 7 dígitos sin verificar módulo 11', () => {
    for (const ruc of ['1793189906001', '0993366721001', '2299981235001']) {
      expect(validateRuc(ruc)).toMatchObject({ valid: true, checksumVerified: false });
    }
  });

  it('valida entidades públicas con el verificador en la novena posición', () => {
    expect(validateRuc('2069999930001')).toMatchObject({ valid: true, checksumVerified: true, taxpayerType: 'PUBLIC_ENTITY' });
    expect(validateRuc('2069999940001')).toMatchObject({ valid: true, checksumVerified: false });
    expect(validateRuc('2069999930000').valid).toBe(false);
  });

  it('rechaza terceros dígitos 7 u 8 y provincias inexistentes', () => {
    expect(validateRuc('2079999930001').valid).toBe(false);
    expect(validateRuc('2599998701001').valid).toBe(false);
    expect(validateRuc('2099998701002').valid).toBe(false);
  });
});

describe('identificationValidator', () => {
  it('expone el mensaje del algoritmo como error del control', () => {
    const control = new FormControl('2059999992', identificationValidator('CEDULA', true));
    expect(control.errors).toEqual({ identification: 'El dígito verificador de la cédula no es correcto.' });
    control.setValue('');
    expect(control.errors).toBeNull();
    expect(new FormControl('', identificationValidator('RUC')).errors).toEqual({ required: true });
  });
});
