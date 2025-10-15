<!-- 27731c7e-fd25-4469-83dc-62fec6a427dd 6a3af9c6-5988-4517-9359-570fbb2c898c -->
# Fix Figure 10b (Monsoon Phases) Formatting

## User Request

"r moonsoom phase er jonno figure 10b er jeita korcho? oita kivabe korcho? blue box er text gula ekdom bottom e plain text hisebe likhe felo. blue box er dorkar nai jeit top right e ache.."

## Current Issue

Figure 10b has a blue info box in the top-right corner with explanation text:

```python
explanation = """Monsoon Phase Patterns:
• Dry Season: Highest drought frequency
• Pre-Monsoon: High drought frequency
• Peak Monsoon: Lower drought frequency
• Post-Monsoon: Lowest drought frequency

Data Source: Real climate data (17,868 records)"""

props = dict(boxstyle='round', facecolor='lightcyan', alpha=0.8)
ax.text(0.98, 0.98, explanation, ...)  # TOP-RIGHT blue box
```

## Required Changes

### 1. Remove Blue Info Box

- Delete the `props = dict(boxstyle='round', facecolor='lightcyan', alpha=0.8)` line
- Delete the `ax.text(0.98, 0.98, explanation, ..., bbox=props)` section

### 2. Add Plain Text at Bottom

Replace with 2 separate plain text lines at the bottom (similar to Figure 10a):

```python
# Line 1: Monsoon phase explanation (horizontal format)
phase_explanation = "Dry Season (Dec-Feb) | Pre-Monsoon (Mar-May) | Peak Monsoon (Jun-Sep) | Post-Monsoon (Oct-Nov)"
ax.text(0.5, -0.15, phase_explanation, transform=ax.transAxes, fontsize=10, 
        ha='center', va='top', style='italic', color='gray')

# Line 2: Data source
data_source = "Data Source: enhanced_temporal_features.csv (17,868 records) - Real monsoon phase drought patterns"
ax.text(0.5, -0.20, data_source, transform=ax.transAxes, fontsize=9, 
        ha='center', va='top', style='italic', color='darkgray')
```

## File to Modify

`/home/alamin/Documents/DroughtClassification/master_drought_pipeline.py`

- Function: `create_figure_10b_clean()` (lines 1695-1743)

## Implementation Steps

1. Remove lines with blue box:

   - `explanation = """..."""` (multi-line string)
   - `props = dict(...)` 
   - `ax.text(0.98, 0.98, ...)` with bbox

2. Add new plain text lines before `plt.tight_layout()`:

   - Phase explanation (horizontal format, centered, below x-axis)
   - Data source (smaller font, gray color)

3. Keep existing:

   - Bar plot with colors
   - Value labels on bars
   - Grid, title, axis labels

## Expected Result

- Clean figure without blue box clutter
- Plain text explanations at bottom (like Figure 10a)
- Professional, publication-ready appearance
- Consistent formatting across Figure 10a and 10b

## Verification

After implementation:

1. Regenerate Figure 10b
2. Check that blue box is removed
3. Verify plain text is centered at bottom
4. Confirm all data is still 100% REAL
5. Update prompts.txt

### To-dos

- [ ] Fix Figure 8 (Feature Importance) with real data
- [ ] Fix Figure 7 (Confusion Matrix) with real data
- [ ] Fix Figure 11 (Prediction Distribution) with real data
- [ ] Fix Figure 9 (SHAP) - may need Phase 6 modification
- [ ] con