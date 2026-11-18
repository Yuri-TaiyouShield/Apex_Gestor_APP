import { TestBed } from '@angular/core/testing';

import { SecureStorageService } from './secure-storage.service';

describe('SecureStorageService', () => {
  let service: SecureStorageService;

  beforeEach(() => {
    localStorage.clear();
    service = TestBed.inject(SecureStorageService);
  });

  afterEach(() => {
    delete window.apexDesktop;
    localStorage.clear();
  });

  it('persiste no localStorage quando não há integração desktop', async () => {
    await service.setItem('apex.refreshToken', 'refresh-legado');

    expect(localStorage.getItem('apex.refreshToken')).toBe('refresh-legado');
    await expectAsync(service.getItem('apex.refreshToken')).toBeResolved('refresh-legado');

    await service.removeItem('apex.refreshToken');
    await expectAsync(service.getItem('apex.refreshToken')).toBeResolved(null);
  });

  it('delega ao armazenamento seguro desktop quando disponível', async () => {
    const secureStorage = {
      getItem: jasmine.createSpy('getItem').and.resolveTo('sealed-refresh'),
      setItem: jasmine.createSpy('setItem').and.resolveTo(true),
      removeItem: jasmine.createSpy('removeItem').and.resolveTo(true)
    };
    window.apexDesktop = { isDesktop: true, secureStorage };

    await service.setItem('apex.refreshToken', 'refresh-novo');
    const stored = await service.getItem('apex.refreshToken');

    expect(secureStorage.setItem).toHaveBeenCalledWith('apex.refreshToken', 'refresh-novo');
    expect(stored).toBe('sealed-refresh');
    expect(localStorage.getItem('apex.refreshToken')).toBeNull();
  });
});
