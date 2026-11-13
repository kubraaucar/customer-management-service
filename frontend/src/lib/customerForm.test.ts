import { describe, expect, it } from 'vitest'
import {
  pickFieldErrors,
  toCustomerInput,
  toFormValues,
  validateCustomer,
  type CustomerFormValues,
} from './customerForm'

const TODAY = '2026-11-13'

const valid: CustomerFormValues = {
  name: 'Kübra',
  surname: 'Uçar',
  citizenNumber: '12345678901',
  birthDate: '2002-01-08',
  active: true,
}

describe('validateCustomer', () => {
  it('accepts a valid customer', () => {
    expect(validateCustomer(valid, TODAY)).toEqual({})
  })

  it('accepts a customer without a birth date', () => {
    expect(validateCustomer({ ...valid, birthDate: '' }, TODAY)).toEqual({})
  })

  it('requires name and surname (spaces only do not count)', () => {
    const errors = validateCustomer({ ...valid, name: '   ', surname: '' }, TODAY)
    expect(errors.name).toBe('Ad boş olamaz.')
    expect(errors.surname).toBe('Soyad boş olamaz.')
  })

  it('limits name and surname to 100 characters', () => {
    const errors = validateCustomer({ ...valid, name: 'a'.repeat(101) }, TODAY)
    expect(errors.name).toBe('Ad en fazla 100 karakter olabilir.')
  })

  it.each([
    ['', 'TC kimlik numarası boş olamaz.'],
    ['1234', 'TC kimlik numarası 11 haneli olmalıdır.'],
    ['123456789012', 'TC kimlik numarası 11 haneli olmalıdır.'],
    ['1234567890a', 'TC kimlik numarası 11 haneli olmalıdır.'],
    ['01234567890', 'TC kimlik numarası 0 ile başlayamaz.'],
  ])('rejects TC number %j', (citizenNumber, message) => {
    expect(validateCustomer({ ...valid, citizenNumber }, TODAY).citizenNumber).toBe(message)
  })

  it('requires the birth date to be in the past', () => {
    const message = 'Doğum tarihi geçmişte bir tarih olmalıdır.'
    expect(validateCustomer({ ...valid, birthDate: TODAY }, TODAY).birthDate).toBe(message)
    expect(validateCustomer({ ...valid, birthDate: '2030-01-01' }, TODAY).birthDate).toBe(message)
    expect(validateCustomer({ ...valid, birthDate: '2026-11-12' }, TODAY).birthDate).toBeUndefined()
  })
})

describe('toCustomerInput', () => {
  it('trims names, turns the TC number into a number and an empty date into null', () => {
    expect(toCustomerInput({ ...valid, name: ' Kübra ', surname: 'Uçar ', birthDate: '' })).toEqual({
      name: 'Kübra',
      surname: 'Uçar',
      citizenNumber: 12345678901,
      birthDate: null,
      active: true,
    })
  })
})

describe('toFormValues', () => {
  it('starts a new customer empty and active', () => {
    expect(toFormValues(null)).toEqual({ name: '', surname: '', citizenNumber: '', birthDate: '', active: true })
  })

  it('fills the form from an existing customer', () => {
    const values = toFormValues({
      id: '1',
      name: 'Ali',
      surname: 'Kaya',
      citizenNumber: 11111111111,
      birthDate: null,
      active: false,
    })
    expect(values).toEqual({ name: 'Ali', surname: 'Kaya', citizenNumber: '11111111111', birthDate: '', active: false })
  })
})

describe('pickFieldErrors', () => {
  it('keeps only errors for fields that the form shows', () => {
    expect(pickFieldErrors({ name: 'Ad boş olamaz.', somethingElse: 'x' })).toEqual({ name: 'Ad boş olamaz.' })
  })
})
