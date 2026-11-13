import { useState, type FormEvent } from 'react'
import { createCustomer, updateCustomer, type Customer } from '../api/customers'
import { ApiError, errorMessage } from '../api/http'
import {
  hasErrors,
  MAX_NAME_LENGTH,
  pickFieldErrors,
  toCustomerInput,
  toFormValues,
  validateCustomer,
  type CustomerField,
  type CustomerFormValues,
  type FieldErrors,
} from '../lib/customerForm'
import { toIsoDate } from '../lib/format'
import { Modal } from './Modal'

interface CustomerFormDialogProps {
  /** The customer to edit, or null to add a new one. */
  customer: Customer | null
  onClose: () => void
  onSaved: (saved: Customer, created: boolean) => void
}

function yesterday(): string {
  const date = new Date()
  date.setDate(date.getDate() - 1)
  return toIsoDate(date)
}

export function CustomerFormDialog({ customer, onClose, onSaved }: CustomerFormDialogProps) {
  const isNew = customer === null
  const [values, setValues] = useState<CustomerFormValues>(() => toFormValues(customer))
  const [errors, setErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitted, setSubmitted] = useState(false)
  const [saving, setSaving] = useState(false)

  function update<K extends CustomerField>(field: K, value: CustomerFormValues[K]) {
    const next = { ...values, [field]: value }
    setValues(next)
    // After the first save attempt, re-check while typing so messages disappear once fixed.
    if (submitted) setErrors(validateCustomer(next))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    setSubmitted(true)
    setFormError(null)

    /** Shows the messages and moves the cursor to the topmost wrong field. */
    function showErrors(found: FieldErrors) {
      setErrors(found)
      // Errors are added in field order, so the first key is the topmost wrong field.
      const firstInvalid = Object.keys(found)[0]
      if (firstInvalid) form.querySelector<HTMLElement>(`#customer-${firstInvalid}`)?.focus()
    }

    const found = validateCustomer(values)
    showErrors(found)
    if (hasErrors(found)) return

    setSaving(true)
    try {
      const input = toCustomerInput(values)
      const saved = isNew ? await createCustomer(input) : await updateCustomer(customer.id, input)
      onSaved(saved, isNew)
    } catch (error) {
      setSaving(false)
      if (error instanceof ApiError && error.status === 409) {
        // The only conflict a customer can cause is a TC number that is already taken.
        showErrors({ citizenNumber: error.message })
      } else if (error instanceof ApiError && hasErrors(pickFieldErrors(error.fieldErrors))) {
        showErrors(pickFieldErrors(error.fieldErrors))
      } else {
        setFormError(errorMessage(error))
      }
    }
  }

  function fieldProps(field: CustomerField) {
    const id = `customer-${field}`
    return {
      id,
      'aria-invalid': errors[field] ? true : undefined,
      'aria-describedby': errors[field] ? `${id}-error` : undefined,
    }
  }

  function fieldError(field: CustomerField) {
    return errors[field] ? (
      <p id={`customer-${field}-error`} className="field-error">
        {errors[field]}
      </p>
    ) : null
  }

  return (
    <Modal title={isNew ? 'Yeni müşteri' : 'Müşteriyi düzenle'} onClose={onClose} busy={saving}>
      <form className="form" onSubmit={handleSubmit} noValidate>
        <div className="form-row">
          <div className="field">
            <label htmlFor="customer-name">Ad</label>
            <input
              {...fieldProps('name')}
              type="text"
              value={values.name}
              maxLength={MAX_NAME_LENGTH}
              autoComplete="off"
              autoFocus
              onChange={(event) => update('name', event.target.value)}
            />
            {fieldError('name')}
          </div>
          <div className="field">
            <label htmlFor="customer-surname">Soyad</label>
            <input
              {...fieldProps('surname')}
              type="text"
              value={values.surname}
              maxLength={MAX_NAME_LENGTH}
              autoComplete="off"
              onChange={(event) => update('surname', event.target.value)}
            />
            {fieldError('surname')}
          </div>
        </div>

        <div className="form-row">
          <div className="field">
            <label htmlFor="customer-citizenNumber">TC kimlik no</label>
            <input
              {...fieldProps('citizenNumber')}
              type="text"
              inputMode="numeric"
              value={values.citizenNumber}
              autoComplete="off"
              placeholder="11 haneli"
              // Spaces and dashes (e.g. when pasted) are dropped first, then the 11-digit limit applies.
              onChange={(event) => update('citizenNumber', event.target.value.replace(/\D/g, '').slice(0, 11))}
            />
            {fieldError('citizenNumber')}
          </div>
          <div className="field">
            <label htmlFor="customer-birthDate">
              Doğum tarihi <span className="optional">(isteğe bağlı)</span>
            </label>
            <input
              {...fieldProps('birthDate')}
              type="date"
              value={values.birthDate}
              max={yesterday()}
              onChange={(event) => update('birthDate', event.target.value)}
            />
            {fieldError('birthDate')}
          </div>
        </div>

        <label className="checkbox">
          <input
            type="checkbox"
            checked={values.active}
            onChange={(event) => update('active', event.target.checked)}
          />
          Aktif müşteri
        </label>

        {formError && (
          <p className="alert alert-error" role="alert">
            {formError}
          </p>
        )}

        <div className="modal-actions">
          <button type="button" className="button" onClick={onClose} disabled={saving}>
            Vazgeç
          </button>
          <button type="submit" className="button button-primary" disabled={saving}>
            {saving ? 'Kaydediliyor…' : 'Kaydet'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
