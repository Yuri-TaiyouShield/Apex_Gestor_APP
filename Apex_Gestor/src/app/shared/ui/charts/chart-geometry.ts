export interface SvgCoord {
  x: number;
  y: number;
}

/**
 * Gera um caminho SVG suave (bezier cubica via Catmull-Rom) evitando serrilhado em series temporais.
 */
export function buildSmoothLinePath(points: readonly SvgCoord[]): string {
  if (points.length === 0) {
    return '';
  }
  if (points.length === 1) {
    return `M ${points[0].x.toFixed(2)} ${points[0].y.toFixed(2)}`;
  }

  const parts: string[] = [`M ${points[0].x.toFixed(2)} ${points[0].y.toFixed(2)}`];
  for (let i = 0; i < points.length - 1; i++) {
    const p0 = points[Math.max(i - 1, 0)];
    const p1 = points[i];
    const p2 = points[i + 1];
    const p3 = points[Math.min(i + 2, points.length - 1)];

    const cp1x = p1.x + (p2.x - p0.x) / 6;
    const cp1y = p1.y + (p2.y - p0.y) / 6;
    const cp2x = p2.x - (p3.x - p1.x) / 6;
    const cp2y = p2.y - (p3.y - p1.y) / 6;

    parts.push(`C ${cp1x.toFixed(2)} ${cp1y.toFixed(2)}, ${cp2x.toFixed(2)} ${cp2y.toFixed(2)}, ${p2.x.toFixed(2)} ${p2.y.toFixed(2)}`);
  }
  return parts.join(' ');
}

export function buildSmoothAreaPath(points: readonly SvgCoord[], baselineY: number): string {
  if (points.length === 0) {
    return '';
  }
  const line = buildSmoothLinePath(points);
  const last = points[points.length - 1];
  const first = points[0];
  return `${line} L ${last.x.toFixed(2)} ${baselineY.toFixed(2)} L ${first.x.toFixed(2)} ${baselineY.toFixed(2)} Z`;
}

/**
 * Calcula valores de referencia legiveis para o eixo Y.
 */
export function computeNiceMax(rawMax: number): number {
  if (!Number.isFinite(rawMax) || rawMax <= 0) {
    return 100;
  }
  const magnitude = Math.pow(10, Math.floor(Math.log10(rawMax)));
  const fraction = rawMax / magnitude;
  let niceFraction = 10;
  if (fraction <= 1) {
    niceFraction = 1;
  } else if (fraction <= 2) {
    niceFraction = 2;
  } else if (fraction <= 5) {
    niceFraction = 5;
  }
  return niceFraction * magnitude;
}

export function polarToCartesian(cx: number, cy: number, radius: number, angleDeg: number): SvgCoord {
  const rad = ((angleDeg - 90) * Math.PI) / 180;
  return {
    x: cx + radius * Math.cos(rad),
    y: cy + radius * Math.sin(rad)
  };
}

/**
 * Gera arco ou fatia de pizza SVG entre dois angulos em graus (0 no topo, sentido horario).
 */
export function describePieSlice(cx: number, cy: number, radius: number, startAngle: number, endAngle: number): string {
  const clampedEnd = endAngle - startAngle >= 359.999 ? startAngle + 359.999 : endAngle;
  const start = polarToCartesian(cx, cy, radius, clampedEnd);
  const end = polarToCartesian(cx, cy, radius, startAngle);
  const largeArc = clampedEnd - startAngle <= 180 ? '0' : '1';
  return `M ${cx.toFixed(2)} ${cy.toFixed(2)} L ${end.x.toFixed(2)} ${end.y.toFixed(2)} A ${radius} ${radius} 0 ${largeArc} 1 ${start.x.toFixed(2)} ${start.y.toFixed(2)} Z`;
}
