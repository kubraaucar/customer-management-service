import { request } from './http'

/** A customer as the backend returns it (CustomerResponse). */
export interface Customer {
  id: string
  name: string
  surname: string
  citizenNumber: number
  /** ISO date (yyyy-MM-dd) or null. */
  birthDate: string | null
  active: boolean
}

/** What we send when creating or updating a customer (CustomerRequest). */
export interface CustomerInput {
  name: string
  surname: string
  citizenNumber: number
  birthDate: string | null
  active: boolean
}

/** Spring Data's PagedModel JSON. */
export interface Page<T> {
  content: T[]
  page: {
    size: number
    number: number
    totalElements: number
    totalPages: number
  }
}

export type SortField = 'name' | 'surname' | 'citizenNumber' | 'birthDate' | 'active'
export type SortDirection = 'asc' | 'desc'

export interface Sort {
  field: SortField
  direction: SortDirection
}

export interface ListParams {
  search: string
  /** Zero-based page number. */
  page: number
  size: number
  sort: Sort
}

const BASE = '/api/customers'

export function listCustomers(params: ListParams, signal?: AbortSignal): Promise<Page<Customer>> {
  const query = new URLSearchParams({ page: String(params.page), size: String(params.size) })
  query.append('sort', `${params.sort.field},${params.sort.direction}`)
  // TC numbers are unique, so they keep the order stable between pages when the main sort has ties.
  if (params.sort.field !== 'citizenNumber') query.append('sort', 'citizenNumber,asc')
  const search = params.search.trim()
  if (search) query.set('search', search)
  return request<Page<Customer>>(`${BASE}?${query}`, { signal })
}

export function createCustomer(input: CustomerInput): Promise<Customer> {
  return request<Customer>(BASE, { method: 'POST', body: JSON.stringify(input) })
}

export function updateCustomer(id: string, input: CustomerInput): Promise<Customer> {
  return request<Customer>(`${BASE}/${id}`, { method: 'PUT', body: JSON.stringify(input) })
}

export function setCustomerActive(id: string, active: boolean): Promise<Customer> {
  return request<Customer>(`${BASE}/${id}/activation`, {
    method: 'PATCH',
    body: JSON.stringify({ active }),
  })
}

export function deleteCustomer(id: string): Promise<void> {
  return request<void>(`${BASE}/${id}`, { method: 'DELETE' })
}
