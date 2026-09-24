import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { buildAppConfig } from './app/app.config';
import { loadConfig } from './app/core/config';

loadConfig()
  .then((config) => bootstrapApplication(App, buildAppConfig(config)))
  .catch((err) => console.error(err));
