interface PaginationProps {
  /** Zero-based page number. */
  page: number
  totalPages: number
  totalElements: number
  pageSize: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, totalElements, pageSize, onPageChange }: PaginationProps) {
  const first = page * pageSize + 1
  const last = Math.min((page + 1) * pageSize, totalElements)

  return (
    <nav className="pagination" aria-label="Sayfalar">
      <p className="pagination-summary">
        {totalElements} müşteriden {first}–{last} arası gösteriliyor
      </p>
      <div className="pagination-controls">
        <button type="button" className="button button-small" onClick={() => onPageChange(page - 1)} disabled={page === 0}>
          ‹ Önceki
        </button>
        <span className="pagination-page">
          Sayfa {page + 1} / {Math.max(totalPages, 1)}
        </span>
        <button
          type="button"
          className="button button-small"
          onClick={() => onPageChange(page + 1)}
          disabled={page + 1 >= totalPages}
        >
          Sonraki ›
        </button>
      </div>
    </nav>
  )
}
