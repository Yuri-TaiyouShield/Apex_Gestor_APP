import { HttpClient } from '@angular/common/http';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { ApiConfigService } from './api-config.service';
import { ApiReadinessService } from './api-readiness.service';

describe('ApiReadinessService', () => {
  let http: jasmine.SpyObj<HttpClient>;
  let service: ApiReadinessService;

  beforeEach(() => {
    http = jasmine.createSpyObj<HttpClient>('HttpClient', ['get']);
    TestBed.configureTestingModule({
      providers: [
        ApiReadinessService,
        { provide: HttpClient, useValue: http },
        { provide: ApiConfigService, useValue: { healthUrl: () => 'http://127.0.0.1:8080/actuator/health' } }
      ]
    });
    service = TestBed.inject(ApiReadinessService);
  });

  it('libera a fila operacional quando a API responde UP no primeiro ping', fakeAsync(() => {
    http.get.and.returnValue(of({ status: 'UP' }));

    let liberated = false;
    service.waitForApi().subscribe(() => { liberated = true; });

    tick();

    expect(liberated).toBeTrue();
    expect(http.get).toHaveBeenCalledWith('http://127.0.0.1:8080/actuator/health');
  }));

  it('repete o polling de saúde até a API voltar', fakeAsync(() => {
    http.get.and.returnValues(
      throwError(() => new Error('offline')) as never,
      of({ status: 'UP' }) as never
    );

    let liberated = false;
    service.waitForApi().subscribe(() => { liberated = true; });

    tick();       // primeiro ping falha
    tick(1500);   // segundo ping responde UP

    expect(liberated).toBeTrue();
    expect(http.get).toHaveBeenCalledTimes(2);
  }));

  it('trata falha de saúde como indisponível e estoura o prazo com API_UNAVAILABLE', fakeAsync(() => {
    http.get.and.returnValue(throwError(() => new Error('down')));

    let error: unknown;
    service.waitForApi(600).subscribe({
      next: () => fail('não deveria liberar'),
      error: (failure: unknown) => { error = failure; }
    });

    tick();
    tick(600);

    expect(error).toBeInstanceOf(Error);
    expect(error instanceof Error ? error.message : '').toBe('API_UNAVAILABLE');
    expect(http.get).toHaveBeenCalledTimes(1);
  }));

  it('considera status diferente de UP como não pronto', fakeAsync(() => {
    http.get.and.returnValue(of({ status: 'DOWN' }));

    let error: unknown;
    service.waitForApi(600).subscribe({
      next: () => fail('não deveria liberar'),
      error: (failure: unknown) => { error = failure; }
    });

    tick();
    tick(600);

    expect(error instanceof Error ? error.message : '').toBe('API_UNAVAILABLE');
  }));
});
