import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Building2,
  Boxes,
  TrendingUp,
  Coins,
  ArrowRight,
  LogIn,
  PlusCircle,
  Menu,
  X,
  Layers,
  FolderKanban,
  ShieldCheck,
  Sparkles
} from 'lucide-react';
import heroBg from '../../assets/hero-bg.jpg';
import '../../styles/landing.css';

export default function LandingPage() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const navigate = useNavigate();

  const scrollToSection = (id) => {
    setMobileMenuOpen(false);
    const element = document.getElementById(id);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <div className="landing-container">
      {/* ===================================================================
          Hero & Navigation Wrapper with Skyscraper Background
          =================================================================== */}
      <section
        className="landing-hero-wrapper"
        style={{ backgroundImage: `url(${heroBg})` }}
      >
        <div className="landing-hero-overlay" />

        {/* Top Navigation Bar */}
        <div className="landing-nav-container">
          <nav className="landing-nav">
            {/* Logo Emblem (Stockwise Systems style from reference) */}
            <Link to="/" className="landing-brand">
              <div className="landing-logo-emblem">
                <Building2 size={24} />
              </div>
              <div className="landing-brand-text">
                <span className="landing-brand-name">STOCKWISE</span>
                <span className="landing-brand-subtitle">SYSTEMS &bull; SIMS</span>
              </div>
            </Link>

            {/* Desktop Nav Links */}
            <div className="landing-nav-links">
              <button
                type="button"
                className="landing-nav-link active"
                onClick={() => scrollToSection('home')}
              >
                Home
              </button>
              <button
                type="button"
                className="landing-nav-link"
                onClick={() => scrollToSection('about')}
              >
                About
              </button>
              <button
                type="button"
                className="landing-nav-link"
                onClick={() => scrollToSection('capabilities')}
              >
                Capabilities
              </button>
              <button
                type="button"
                className="landing-nav-link"
                onClick={() => scrollToSection('capabilities')}
              >
                Contact Us
              </button>
            </div>

            {/* Top Right Action Buttons */}
            <div className="landing-nav-actions">
              <Link to="/login" className="btn-nav-signin">
                Sign In
              </Link>
              <Link to="/create-inventory" className="btn-nav-create">
                Create Inventory
              </Link>
              <button
                type="button"
                className="landing-mobile-toggle"
                onClick={() => setMobileMenuOpen(true)}
                aria-label="Open navigation menu"
              >
                <Menu size={22} />
              </button>
            </div>
          </nav>

          {/* Thin Horizontal Divider Line spanning across navbar (from Reference video) */}
          <div className="landing-divider-line" />
        </div>

        {/* Hero Content Section (Reference Image 2) */}
        <div id="home" className="landing-hero-content">
          <div className="hero-text-block">
            <div className="hero-pill-tag">
              <Sparkles size={14} style={{ color: '#818cf8' }} />
              <span>Next-Gen Enterprise Inventory Platform</span>
            </div>

            <h1 className="hero-headline">
              Experience the Power of Informed Inventory
            </h1>

            <p className="hero-subhead">
              Build a Strong Foundation for Your Inventory, Operations, and Real-Time Forecasting Today.
            </p>

            <div className="hero-cta-group">
              <Link to="/create-inventory" className="btn-hero-primary">
                <span>Create Inventory</span>
                <ArrowRight size={18} />
              </Link>

              <Link to="/login" className="btn-hero-secondary">
                <LogIn size={18} />
                <span>Sign In to Workspace</span>
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* ===================================================================
          "Who Are We?" Section (Reference Image 3)
          =================================================================== */}
      <section id="about" className="landing-about-section">
        <div className="landing-about-container">
          <h2 className="about-headline">Who Are We?</h2>
          <p className="about-description">
            We are driven by a commitment to delivering exceptional inventory, procurement, and warehouse outcomes. We leverage a data-driven approach and a deep understanding of market dynamics to generate sustainable efficiency, eliminate stockouts, and maximize working capital for our clients.
          </p>
        </div>
      </section>

      {/* ===================================================================
          "Our Capabilities" / Services Section (Reference Image 3)
          =================================================================== */}
      <section id="capabilities" className="landing-services-section">
        <div className="landing-services-container">
          <div className="services-header">
            <h2 className="services-headline">Our Capabilities</h2>
            <p className="services-subhead">
              End-to-end supply chain orchestration built for growing teams and high-throughput enterprise operations.
            </p>
          </div>

          <div className="services-grid">
            {/* Capability 1: Warehouse & Inventory Control */}
            <div className="service-card">
              <div className="service-icon-box">
                <FolderKanban size={32} />
              </div>
              <h3 className="service-card-title">Warehouse & Inventory Control</h3>
              <p className="service-card-desc">
                Real-time multi-location tracking, automated batch reconciliation, SKU cataloging, and precise safety threshold alerting across your full catalog.
              </p>
            </div>

            {/* Capability 2: Demand Forecasting & Analytics */}
            <div className="service-card">
              <div className="service-icon-box">
                <TrendingUp size={32} />
              </div>
              <h3 className="service-card-title">Demand Forecasting & Insights</h3>
              <p className="service-card-desc">
                Predictive analytics calculating stockout risk, velocity metrics, reorder timelines, and seasonal demand peaks to protect operational margins.
              </p>
            </div>

            {/* Capability 3: Working Capital & Purchasing */}
            <div className="service-card">
              <div className="service-icon-box">
                <Coins size={32} />
              </div>
              <h3 className="service-card-title">Purchasing & Capital Optimization</h3>
              <p className="service-card-desc">
                Streamlined purchase orders, supplier contract auditing, automated delivery receiving, and valuation reports to keep working capital optimized.
              </p>
            </div>
          </div>

          {/* Bottom Action Bar (matching Reference Image 3 bottom buttons) */}
          <div className="landing-bottom-bar">
            <Link to="/login" className="btn-bottom-outline">
              <LogIn size={16} />
              <span>Sign In to Existing Workspace</span>
            </Link>

            <Link to="/create-inventory" className="btn-bottom-solid">
              <PlusCircle size={16} />
              <span>Create New Inventory Workspace</span>
            </Link>
          </div>
        </div>
      </section>

      {/* ===================================================================
          Footer
          =================================================================== */}
      <footer className="landing-footer">
        <div className="landing-footer-container">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Building2 size={18} style={{ color: '#818cf8' }} />
            <span>&copy; {new Date().getFullYear()} Stockwise Systems &bull; Smart Inventory Management System. All rights reserved.</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', color: '#64748b' }}>
            <span>PostgreSQL Active</span>
            <span>Spring Boot 4.1.1</span>
            <span>Enterprise Edition v1.0.0</span>
          </div>
        </div>
      </footer>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="landing-mobile-drawer">
          <button
            type="button"
            className="landing-mobile-close"
            onClick={() => setMobileMenuOpen(false)}
            aria-label="Close menu"
          >
            <X size={24} />
          </button>

          <Link
            to="/"
            className="landing-brand"
            style={{ marginBottom: '1rem' }}
            onClick={() => setMobileMenuOpen(false)}
          >
            <div className="landing-logo-emblem">
              <Building2 size={24} />
            </div>
            <div className="landing-brand-text">
              <span className="landing-brand-name">STOCKWISE</span>
              <span className="landing-brand-subtitle">SYSTEMS</span>
            </div>
          </Link>

          <button
            type="button"
            className="landing-nav-link"
            style={{ textAlign: 'left', fontSize: '1.2rem' }}
            onClick={() => scrollToSection('home')}
          >
            Home
          </button>
          <button
            type="button"
            className="landing-nav-link"
            style={{ textAlign: 'left', fontSize: '1.2rem' }}
            onClick={() => scrollToSection('about')}
          >
            About
          </button>
          <button
            type="button"
            className="landing-nav-link"
            style={{ textAlign: 'left', fontSize: '1.2rem' }}
            onClick={() => scrollToSection('capabilities')}
          >
            Capabilities
          </button>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '2rem' }}>
            <Link
              to="/login"
              className="btn-hero-secondary"
              onClick={() => setMobileMenuOpen(false)}
            >
              Sign In
            </Link>
            <Link
              to="/create-inventory"
              className="btn-hero-primary"
              onClick={() => setMobileMenuOpen(false)}
            >
              Create Inventory
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
