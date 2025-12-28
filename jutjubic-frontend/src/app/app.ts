import { Component, OnInit } from '@angular/core';
import { PingService } from './services/ping.service';

@Component({
  selector: 'app-root',
  standalone: true,
  template: `
    <h1>Jutjubić frontend</h1>
    <p style="font-size:40px;color:red">OVO JE NOVI TEMPLATE</p>

    <p><b>Backend kaže:</b> {{ msg }}</p>

    <h2>TEST RENDER</h2>
  `
})
export class App implements OnInit {
  msg = 'učitavanje...';

  constructor(private pingService: PingService) {}

  ngOnInit(): void {
    console.log('NGONINIT');
    this.pingService.ping().subscribe({
      next: (res) => {
        console.log('PING NEXT:', res);
        this.msg = res;
      },
      error: (err) => {
        console.error('PING ERROR:', err);
        this.msg = 'GREŠKA';
      }
    });
  }
}
