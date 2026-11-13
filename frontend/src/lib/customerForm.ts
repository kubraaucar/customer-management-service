import type { Customer, CustomerInput } from '../api/customers'
import { toIsoDate } from './format'

/** The form keeps everything as text, exactly as the user typed it. */
export interface CustomerFormValues {
  name: string
  surname: string
  citizenNumber: string
  birthDate: string
  active: boolean
}

export type CustomerField = keyof CustomerFormValues
export type FieldErrors = Partial<Record<CustomerField, string>>

export const MAX_NAME_LENGTH = 100
const FIELDS: CustomerField[] = ['name', 'surname', 'citizenNumber', 'birthDate', 'active']

export function toFormValues(customer: Customer | null): CustomerFormValues {
  return {
    name: customer?.name ?? '',
    surname: customer?.surname ?? '',
    citizenNumber: customer ? String(customer.citizenNumber) : '',
    birthDate: customer?.birthDate ?? '',
    active: customer?.active ?? true,
  }
}

export function toCustomerInput(values: CustomerFormValues): CustomerInput {
  return {
    name: values.name.trim(),
    surname: values.surname.trim(),
    citizenNumber: Number(values.citizenNumber),
    birthDate: values.birthDate || null,
    active: values.active,
  }
}

/**
 * Same rules as the backend's CustomerRequest, with the same Turkish messages,
 * so most mistakes are shown before anything is sent.
 */
export function validateCustomer(values: CustomerFormValues, today: string = toIsoDate(new Date())): FieldErrors {
  const errors: FieldErrors = {}

  const name = values.name.trim()
  if (!name) errors.name = 'Ad boş olamaz.'
  else if (name.length > MAX_NAME_LENGTH) errors.name = `Ad en fazla ${MAX_NAME_LENGTH} karakter olabilir.`

  const surname = values.surname.trim()
  if (!surname) errors.surname = 'Soyad boş olamaz.'
  else if (surname.length > MAX_NAME_LENGTH) errors.surname = `Soyad en fazla ${MAX_NAME_LENGTH} karakter olabilir.`

  const citizenNumber = values.citizenNumber.trim()
  if (!citizenNumber) errors.citizenNumber = 'TC kimlik numarası boş olamaz.'
  else if (!/^\d{11}$/.test(citizenNumber)) errors.citizenNumber = 'TC kimlik numarası 11 haneli olmalıdır.'
  else if (citizenNumber.startsWith('0')) errors.citizenNumber = 'TC kimlik numarası 0 ile başlayamaz.'

  if (values.birthDate && values.birthDate >= today) {
    errors.birthDate = 'Doğum tarihi geçmişte bir tarih olmalıdır.'
  }

  return errors
}

export function hasErrors(errors: FieldErrors): boolean {
  return Object.keys(errors).length > 0
}

/** Keeps only the backend field errors that belong to fields shown in the form. */
export function pickFieldErrors(serverErrors: Record<string, string>): FieldErrors {
  const errors: FieldErrors = {}
  for (const field of FIELDS) {
    if (serverErrors[field]) errors[field] = serverErrors[field]
  }
  return errors
}
