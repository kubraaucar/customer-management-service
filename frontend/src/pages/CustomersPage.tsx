import { useCallback, useEffect, useState } from 'react'
import {
  deleteCustomer,
  listCustomers,
  setCustomerActive,
  type Customer,
  type Page,
  type Sort,
} from '../api/customers'
import { ApiError, errorMessage } from '../api/http'
import { ROLE_LABELS, type Session } from '../auth/session'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { CustomerFormDialog } from '../components/CustomerFormDialog'
import { CustomerTable } from '../components/CustomerTable'
import { Pagination } from '../components/Pagination'
import { Toast, type ToastMessage } from '../components/Toast'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { fullName } from '../lib/format'

const PAGE_SIZE = 10
const DEFAULT_SORT: Sort = { field: 'name', direction: 'asc' }

/** Which window is open: the add/edit form, the delete confirmation, or none. */
type OpenDialog =
  | { type: 'create' }
  | { type: 'edit'; customer: Customer }
  | { type: 'delete'; customer: Customer }
  | null

interface CustomersPageProps {
  session: Session
  onLogout: () => void
}

export function CustomersPage({ session, onLogout }: CustomersPageProps) {
  // Demo users may only look; the backend enforces this too.
  const canEdit = session.role === 'ADMIN'

  const [searchText, setSearchText] = useState('')
  const search = useDebouncedValue(searchText.trim(), 300)
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<Sort>(DEFAULT_SORT)
  const [reloadCount, setReloadCount] = useState(0)

  const [data, setData] = useState<Page<Customer> | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  // The list is "loading" until an answer has arrived for exactly the current search/page/sort.
  const requestKey = JSON.stringify([search, page, sort, reloadCount])
  const [settledKey, setSettledKey] = useState<string | null>(null)
  const loading = settledKey !== requestKey

  const [dialog, setDialog] = useState<OpenDialog>(null)
  const [pendingIds, setPendingIds] = useState<ReadonlySet<string>>(new Set())
  const [toast, setToast] = useState<ToastMessage | null>(null)

  // A new search always starts from the first page.
  const [lastSearch, setLastSearch] = useState(search)
  if (search !== lastSearch) {
    setLastSearch(search)
    setPage(0)
  }

  const showToast = useCallback((kind: ToastMessage['kind'], text: string) => {
    setToast({ id: Date.now(), kind, text })
  }, [])
  const dismissToast = useCallback(() => setToast(null), [])
  const reload = () => setReloadCount((count) => count + 1)

  useEffect(() => {
    const controller = new AbortController()
    listCustomers({ search, page, size: PAGE_SIZE, sort }, controller.signal)
      .then((result) => {
        // e.g. the last customer on the last page was deleted: go back to the new last page.
        if (page > 0 && page >= result.page.totalPages) {
          setPage(Math.max(result.page.totalPages - 1, 0))
          return
        }
        setData(result)
        setLoadError(null)
        setSettledKey(requestKey)
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        setLoadError(errorMessage(error))
        setSettledKey(requestKey)
      })
    return () => controller.abort()
  }, [search, page, sort, reloadCount, requestKey])

  function handleSortChange(next: Sort) {
    setSort(next)
    setPage(0)
  }

  async function handleToggleActive(customer: Customer) {
    setPendingIds((ids) => new Set(ids).add(customer.id))
    try {
      const updated = await setCustomerActive(customer.id, !customer.active)
      setData((current) =>
        current && {
          ...current,
          content: current.content.map((item) => (item.id === updated.id ? updated : item)),
        },
      )
      showToast('success', `${fullName(updated)} ${updated.active ? 'aktif' : 'pasif'} yapıldı.`)
    } catch (error) {
      showToast('error', errorMessage(error))
      if (error instanceof ApiError && error.status === 404) reload()
    } finally {
      setPendingIds((ids) => {
        const next = new Set(ids)
        next.delete(customer.id)
        return next
      })
    }
  }

  function handleSaved(saved: Customer, created: boolean) {
    setDialog(null)
    showToast('success', `${fullName(saved)} ${created ? 'eklendi' : 'güncellendi'}.`)
    reload()
  }

  async function handleDelete(customer: Customer) {
    try {
      await deleteCustomer(customer.id)
      showToast('success', `${fullName(customer)} silindi.`)
    } catch (error) {
      // Already gone (someone else deleted it): nothing left to do, just refresh the list.
      if (!(error instanceof ApiError && error.status === 404)) throw error
      showToast('success', `${fullName(customer)} zaten silinmiş.`)
    }
    setDialog(null)
    reload()
  }

  const customers = data?.content ?? []
  const firstLoad = data === null

  return (
    <>
      <header className="topbar">
        <div className="topbar-inner">
          <span className="brand">
            <img src="/favicon.svg" alt="" width="28" height="28" />
            Müşteri Yönetimi
          </span>
          <div className="user-menu">
            <span className="user-name">{session.username}</span>
            <span className={`role-badge role-${session.role.toLowerCase()}`}>{ROLE_LABELS[session.role]}</span>
            <button type="button" className="button button-small" onClick={onLogout}>
              Çıkış yap
            </button>
          </div>
        </div>
      </header>

      <div className="app">
        <div className="app-header">
          <div>
            <h1>Müşteriler</h1>
            <p className="subtitle">
              {canEdit
                ? 'Müşteri kayıtlarını arayın, ekleyin, düzenleyin ve silin.'
                : 'Müşteri kayıtlarını arayın ve görüntüleyin.'}
            </p>
          </div>
          {canEdit && (
            <button type="button" className="button button-primary" onClick={() => setDialog({ type: 'create' })}>
              + Yeni müşteri
            </button>
          )}
        </div>

        {!canEdit && (
          <p className="alert alert-info">
            Demo hesabıyla giriş yaptınız. Müşterileri görüntüleyip arayabilirsiniz; ekleme, düzenleme ve silme kapalı.
          </p>
        )}

        <main className="card">
          <div className="toolbar">
            <label className="search">
              <span className="visually-hidden">Müşteri ara</span>
              <input
                type="search"
                placeholder="Ad veya soyada göre ara…"
                value={searchText}
                onChange={(event) => setSearchText(event.target.value)}
              />
            </label>
            {loading && !firstLoad && (
              <span className="loading-hint" aria-live="polite">
                Yükleniyor…
              </span>
            )}
          </div>

          {loadError && (
            <div className="alert alert-error" role="alert">
              <span>{loadError}</span>
              <button type="button" className="button button-small" onClick={reload}>
                Tekrar dene
              </button>
            </div>
          )}

          {firstLoad && !loadError && <p className="empty">Yükleniyor…</p>}

          {!firstLoad && customers.length === 0 && !loadError && (
            <p className="empty">
              {search
                ? `“${search}” için sonuç bulunamadı.`
                : canEdit
                  ? 'Henüz müşteri yok. İlk müşteriyi eklemek için “Yeni müşteri” düğmesine tıklayın.'
                  : 'Henüz müşteri yok.'}
            </p>
          )}

          {customers.length > 0 && data && (
            <>
              <div className={loading ? 'table-wrapper is-loading' : 'table-wrapper'}>
                <CustomerTable
                  customers={customers}
                  sort={sort}
                  onSortChange={handleSortChange}
                  readOnly={!canEdit}
                  pendingIds={pendingIds}
                  onToggleActive={handleToggleActive}
                  onEdit={(customer) => setDialog({ type: 'edit', customer })}
                  onDelete={(customer) => setDialog({ type: 'delete', customer })}
                />
              </div>
              <Pagination
                page={data.page.number}
                totalPages={data.page.totalPages}
                totalElements={data.page.totalElements}
                pageSize={data.page.size}
                onPageChange={setPage}
              />
            </>
          )}
        </main>
      </div>

      {(dialog?.type === 'create' || dialog?.type === 'edit') && (
        <CustomerFormDialog
          key={dialog.type === 'edit' ? dialog.customer.id : 'new'}
          customer={dialog.type === 'edit' ? dialog.customer : null}
          onClose={() => setDialog(null)}
          onSaved={handleSaved}
        />
      )}

      {dialog?.type === 'delete' && (
        <ConfirmDialog
          title="Müşteriyi sil"
          confirmLabel="Sil"
          busyLabel="Siliniyor…"
          onConfirm={() => handleDelete(dialog.customer)}
          onClose={() => setDialog(null)}
        >
          <p>
            <strong>{fullName(dialog.customer)}</strong> (TC {dialog.customer.citizenNumber}) kalıcı olarak silinecek.
            Bu işlem geri alınamaz.
          </p>
        </ConfirmDialog>
      )}

      <Toast toast={toast} onDismiss={dismissToast} />
    </>
  )
}
