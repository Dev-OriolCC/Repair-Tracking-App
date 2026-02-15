# Tracking Software App


## Features

- 📊 **Rich Visualizations** - Powered by VisActor, including bar charts, gauge charts, circle packing charts, and more
- 🌗 **Dark Mode** - Seamless dark/light mode switching with system preference support
- 📱 **Responsive Design** - Fully responsive layout that works on all devices
- 🎨 **Beautiful UI** - Modern and clean interface built with Tailwind CSS
- ⚡️ **Next.js 15** - Built on the latest Next.js features and best practices
- 🔄 **State Management** - Efficient state management with Jotai
- 📦 **Component Library** - Includes Shadcn components styled with Tailwind

## Tech Stack

- [Next.js](https://nextjs.org/) - React framework
- [VisActor](https://visactor.io/) - Visualization library
- [Tailwind CSS](https://tailwindcss.com/) - CSS framework
- [Shadcn](https://ui.shadcn.com/) - UI components
- [Jotai](https://jotai.org/) - State management
- [TypeScript](https://www.typescriptlang.org/) - Type safety

## Quick Start

You can deploy this template to Vercel by clicking the button above, or clone this repository and run it locally.

[Github Repo](https://github.com/mengxi-ream/visactor-next-template)

1. Clone this repository

```bash
git clone https://github.com/mengxi-ream/visactor-next-template
```

2. Install dependencies

```bash
pnpm install
```

3. Run the development server

```bash
pnpm dev
```

4. Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

## Project Structure

```bash
src/
├── app/ # App router pages
├── components/ # React components
│ ├── chart-blocks/ # Chart components
│ ├── nav/ # Navigation components
│ └── ui/ # UI components
├── config/ # Configuration files
├── data/ # Sample data
├── hooks/ # Custom hooks
├── lib/ # Utility functions
├── style/ # Global style
└── types/ # TypeScript types
```

## Charts

This template includes several chart examples:

- Average Tickets Created (Bar Chart)
- Ticket by Channels (Gauge Chart)
- Conversions (Circle Packing Chart)
- Customer Satisfaction (Linear Progress)
- Metrics Overview
