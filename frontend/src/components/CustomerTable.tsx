import type { Customer, Sort, SortField } from '../api/customers'
import { formatDate, fullName } from '../lib/format'

interface CustomerTableProps {
  customers: Customer[]
  sort: Sort
  onSortChange: (sort: Sort) => void
  /** Hides the switch and the edit/delete buttons (demo users). */
  readOnly: boolean
  /** Ids of customers whose active state is being saved right now. */
  pendingIds: ReadonlySet<string>
  onToggleActive: (customer: Customer) => void
  onEdit: (customer: Customer) => void
  onDelete: (customer: Customer) => void
}

const COLUMNS: { field: SortField; label: string }[] = [
  { field: 'name', label: 'Ad' },
  { field: 'surname', label: 'Soyad' },
  { field: 'citizenNumber', label: 'TC kimlik no' },
  { field: 'birthDate', label: 'Doğum tarihi' },
  { field: 'active', label: 'Durum' },
]

export function CustomerTable({
  customers,
  sort,
  onSortChange,
  readOnly,
  pendingIds,
  onToggleActive,
  onEdit,
  onDelete,
}: CustomerTableProps) {
  function handleSort(field: SortField) {
    const direction = sort.field === field && sort.direction === 'asc' ? 'desc' : 'asc'
    onSortChange({ field, direction })
  }

  return (
    <table className="table">
      <thead>
        <tr>
          {COLUMNS.map(({ field, label }) => {
            const active = sort.field === field
            return (
              <th
                key={field}
                scope="col"
                aria-sort={active ? (sort.direction === 'asc' ? 'ascending' : 'descending') : undefined}
              >
                <button type="button" className="sort-button" onClick={() => handleSort(field)}>
                  {label}
                  <span className="sort-icon" aria-hidden="true">
                    {active ? (sort.direction === 'asc' ? '▲' : '▼') : '↕'}
                  </span>
                </button>
              </th>
            )
          })}
          {!readOnly && (
            <th scope="col" className="actions-column">
              <span className="visually-hidden">İşlemler</span>
            </th>
          )}
        </tr>
      </thead>
      <tbody>
        {customers.map((customer) => (
          <tr key={customer.id}>
            <td data-label="Ad">{customer.name}</td>
            <td data-label="Soyad">{customer.surname}</td>
            <td data-label="TC kimlik no" className="numeric">
              {customer.citizenNumber}
            </td>
            <td data-label="Doğum tarihi" className="numeric">
              {formatDate(customer.birthDate)}
            </td>
            <td data-label="Durum">
              {readOnly ? (
                <span className={customer.active ? 'badge badge-active' : 'badge'}>
                  {customer.active ? 'Aktif' : 'Pasif'}
                </span>
              ) : (
                <label className="switch">
                  <input
                    type="checkbox"
                    role="switch"
                    checked={customer.active}
                    disabled={pendingIds.has(customer.id)}
                    aria-label={`${fullName(customer)} aktif`}
                    onChange={() => onToggleActive(customer)}
                  />
                  <span className="switch-track" aria-hidden="true" />
                  <span className={customer.active ? 'badge badge-active' : 'badge'}>
                    {customer.active ? 'Aktif' : 'Pasif'}
                  </span>
                </label>
              )}
            </td>
            {!readOnly && (
              <td className="actions-column">
                <div className="row-actions">
                  <button
                    type="button"
                    className="button button-small"
                    onClick={() => onEdit(customer)}
                    aria-label={`${fullName(customer)} düzenle`}
                  >
                    Düzenle
                  </button>
                  <button
                    type="button"
                    className="button button-small button-danger-outline"
                    onClick={() => onDelete(customer)}
                    aria-label={`${fullName(customer)} sil`}
                  >
                    Sil
                  </button>
                </div>
              </td>
            )}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
