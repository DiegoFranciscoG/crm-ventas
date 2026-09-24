/**
 * Vista previa de totales en el navegador: misma fórmula que el backend (QuoteItem), que es quien calcula el
 * valor definitivo. Redondeo HALF_UP a 2 decimales en cada paso, en centavos para evitar errores de coma flotante.
 */
export interface LineInput {
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  vatPercentage: number;
}

export interface LineResult {
  gross: number;
  discount: number;
  net: number;
  tax: number;
  total: number;
}

const round2 = (value: number) => Math.round((value + Number.EPSILON) * 100) / 100;

export function computeLine(line: LineInput): LineResult {
  const gross = round2(line.quantity * line.unitPrice);
  const discount = round2((gross * line.discountPercent) / 100);
  const net = round2(gross - discount);
  const tax = round2((net * line.vatPercentage) / 100);
  return { gross, discount, net, tax, total: round2(net + tax) };
}

export function computeTotals(lines: LineInput[]) {
  const results = lines.map(computeLine);
  const sum = (key: keyof LineResult) => round2(results.reduce((acc, r) => acc + r[key], 0));
  const byRate = new Map<number, { base: number; tax: number }>();
  lines.forEach((line, i) => {
    const entry = byRate.get(line.vatPercentage) ?? { base: 0, tax: 0 };
    entry.base = round2(entry.base + results[i].net);
    entry.tax = round2(entry.tax + results[i].tax);
    byRate.set(line.vatPercentage, entry);
  });
  return {
    lines: results,
    subtotal: sum('gross'),
    discount: sum('discount'),
    tax: sum('tax'),
    total: round2(sum('net') + sum('tax')),
    byRate: [...byRate.entries()].sort((a, b) => b[0] - a[0]).map(([rate, v]) => ({ rate, ...v })),
  };
}
