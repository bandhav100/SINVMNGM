/**
 * Utility functions for exporting tabular reports to CSV and triggering browser downloads.
 */

export function downloadCSV(filename, headers, rows) {
  // Convert rows and headers into RFC 4180 CSV strings
  const escapeCell = (val) => {
    if (val === null || val === undefined) return '""';
    const str = String(val).replace(/"/g, '""');
    return `"${str}"`;
  };

  const headerRow = headers.map(h => escapeCell(h.label || h)).join(',');
  const dataRows = rows.map(row => {
    return headers.map(h => {
      const key = h.key || h;
      const val = typeof h.formatter === 'function' ? h.formatter(row[key], row) : row[key];
      return escapeCell(val);
    }).join(',');
  });

  // Prepend UTF-8 BOM so Microsoft Excel correctly detects UTF-8 encoding (including ₹ symbol)
  const csvContent = '\uFEFF' + [headerRow, ...dataRows].join('\r\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);
  link.setAttribute('href', url);
  link.setAttribute('download', filename.endsWith('.csv') ? filename : `${filename}.csv`);
  link.style.visibility = 'hidden';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export function printReport() {
  window.print();
}
