import { HttpClient, HttpHandler, HttpRequest, HTTP_INTERCEPTORS } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SpinnerService } from '../enterprise/shared/service/spinner.service';
import { SpinnerInterceptor } from './SpinnerInterceptor';

describe('SpinnerInterceptor', () => {
  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;
  let spinnerService: SpinnerService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [{ provide: HTTP_INTERCEPTORS, useClass: SpinnerInterceptor, multi: true }]
    });
    httpClient = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
    spinnerService = TestBed.inject(SpinnerService);
  });
  afterEach(() => httpTestingController.verify());

  it('keeps blocking while a second request is pending and releases after its failure', () => {
    httpClient.get('/first').subscribe();
    httpClient.get('/second').subscribe({ error: () => undefined });
    expect(spinnerService.isLoading).toBeTrue();
    httpTestingController.expectOne('/first').flush({});
    expect(spinnerService.isLoading).toBeTrue();
    httpTestingController.expectOne('/second').flush({}, { status: 500, statusText: 'Error' });
    expect(spinnerService.isLoading).toBeFalse();
  });

  it('releases cancelled requests', () => {
    const subscription = httpClient.get('/cancel').subscribe();
    const request = httpTestingController.expectOne('/cancel');
    subscription.unsubscribe();
    expect(request.cancelled).toBeTrue();
    expect(spinnerService.isLoading).toBeFalse();
  });

  it('does not leave a lock when a handler throws before returning an observable', () => {
    const interceptor = new SpinnerInterceptor(spinnerService);
    const handler: HttpHandler = { handle: () => { throw new Error('Handler failed'); } };
    const response = interceptor.intercept(new HttpRequest('GET', '/throw'), handler);
    expect(spinnerService.isLoading).toBeFalse();
    response.subscribe({ error: () => undefined });
    expect(spinnerService.isLoading).toBeFalse();
  });
});
