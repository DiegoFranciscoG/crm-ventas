import { computeLine, computeTotals } from './quote-math';

describe('quote-math', () => {
  it('redondea HALF_UP como el backend (99,99 × 15 % = 15,00)', () => {
    expect(computeLine({ quantity: 1, unitPrice: 99.99, discountPercent: 0, vatPercentage: 15 })).toEqual({
      gross: 99.99,
      discount: 0,
      net: 99.99,
      tax: 15,
      total: 114.99,
    });
  });

  it('aplica el descuento antes del IVA y desglosa por tarifa', () => {
    const totals = computeTotals([
      { quantity: 3, unitPrice: 180, discountPercent: 10, vatPercentage: 15 },
      { quantity: 2, unitPrice: 45, discountPercent: 0, vatPercentage: 0 },
      { quantity: 1, unitPrice: 99.99, discountPercent: 0, vatPercentage: 15 },
    ]);
    // Mismo caso que PipelineQuoteIntegrationTest en el backend.
    expect(totals.subtotal).toBe(729.99);
    expect(totals.discount).toBe(54);
    expect(totals.tax).toBe(87.9);
    expect(totals.total).toBe(763.89);
    expect(totals.byRate).toEqual([
      { rate: 15, base: 585.99, tax: 87.9 },
      { rate: 0, base: 90, tax: 0 },
    ]);
  });
});
