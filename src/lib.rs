use kurbo::{BezPath, Point, PathEl};
use linesweeper::{binary_op, BinaryOp, FillRule as LsFillRule, Error as LsError};
use std::sync::Mutex;

#[derive(uniffi::Object)]
pub struct BezierPath {
    path: Mutex<BezPath>,
}

#[derive(uniffi::Enum)]
pub enum BooleanOperation {
    Union,
    Intersection,
    Difference,
    Xor,
}

#[derive(uniffi::Enum)]
pub enum FillRule {
    EvenOdd,
    NonZero,
}

#[derive(uniffi::Enum)]
pub enum PathSegment {
    MoveTo { x: f64, y: f64 },
    LineTo { x: f64, y: f64 },
    QuadTo { cp_x: f64, cp_y: f64, x: f64, y: f64 },
    CurveTo { cp1_x: f64, cp1_y: f64, cp2_x: f64, cp2_y: f64, x: f64, y: f64 },
    ClosePath,
}

#[derive(uniffi::Error, thiserror::Error, Debug)]
pub enum LineSweeperError {
    #[error("Input contained infinite values")]
    InfiniteInput,
    #[error("Input contained NaN values")]
    NaNInput,
    #[error("Linesweeper internal error: {0}")]
    InternalError(String),
}

impl From<LsError> for LineSweeperError {
    fn from(err: LsError) -> Self {
        match err {
            LsError::Infinity => LineSweeperError::InfiniteInput,
            LsError::NaN => LineSweeperError::NaNInput,
            _ => LineSweeperError::InternalError(format!("{:?}", err)),
        }
    }
}

impl From<BooleanOperation> for BinaryOp {
    fn from(op: BooleanOperation) -> Self {
        match op {
            BooleanOperation::Union => BinaryOp::Union,
            BooleanOperation::Intersection => BinaryOp::Intersection,
            BooleanOperation::Difference => BinaryOp::Difference,
            BooleanOperation::Xor => BinaryOp::Xor,
        }
    }
}

impl From<FillRule> for LsFillRule {
    fn from(rule: FillRule) -> Self {
        match rule {
            FillRule::EvenOdd => LsFillRule::EvenOdd,
            FillRule::NonZero => LsFillRule::NonZero,
        }
    }
}

#[uniffi::export]
impl BezierPath {
    /// Create a new empty BezierPath
    #[uniffi::constructor]
    pub fn new() -> Self {
        Self {
            path: Mutex::new(BezPath::new()),
        }
    }
}

impl BezierPath {
    /// Create a BezierPath from an existing kurbo BezPath
    fn from_kurbo_path(kurbo_path: BezPath) -> Self {
        Self {
            path: Mutex::new(kurbo_path),
        }
    }
}

#[uniffi::export]
impl BezierPath {

    /// Move to a point without drawing
    pub fn move_to(&self, x: f64, y: f64) {
        self.path.lock().unwrap().move_to(Point::new(x, y));
    }

    /// Draw a line to a point
    pub fn line_to(&self, x: f64, y: f64) {
        self.path.lock().unwrap().line_to(Point::new(x, y));
    }

    /// Draw a cubic Bezier curve
    pub fn curve_to(&self, cp1_x: f64, cp1_y: f64, cp2_x: f64, cp2_y: f64, x: f64, y: f64) {
        self.path.lock().unwrap().curve_to(
            Point::new(cp1_x, cp1_y),
            Point::new(cp2_x, cp2_y),
            Point::new(x, y),
        );
    }

    /// Draw a quadratic Bezier curve
    pub fn quad_to(&self, cp_x: f64, cp_y: f64, x: f64, y: f64) {
        self.path.lock().unwrap().quad_to(
            Point::new(cp_x, cp_y),
            Point::new(x, y),
        );
    }

    /// Close the current path
    pub fn close_path(&self) {
        self.path.lock().unwrap().close_path();
    }

    /// Get all segments in the path
    pub fn get_segments(&self) -> Vec<PathSegment> {
        let path = self.path.lock().unwrap();
        let mut segments = Vec::new();

        for el in path.elements() {
            match el {
                PathEl::MoveTo(p) => {
                    segments.push(PathSegment::MoveTo { x: p.x, y: p.y });
                }
                PathEl::LineTo(p) => {
                    segments.push(PathSegment::LineTo { x: p.x, y: p.y });
                }
                PathEl::QuadTo(p1, p2) => {
                    segments.push(PathSegment::QuadTo {
                        cp_x: p1.x,
                        cp_y: p1.y,
                        x: p2.x,
                        y: p2.y,
                    });
                }
                PathEl::CurveTo(p1, p2, p3) => {
                    segments.push(PathSegment::CurveTo {
                        cp1_x: p1.x,
                        cp1_y: p1.y,
                        cp2_x: p2.x,
                        cp2_y: p2.y,
                        x: p3.x,
                        y: p3.y,
                    });
                }
                PathEl::ClosePath => {
                    segments.push(PathSegment::ClosePath);
                }
            }
        }

        segments
    }
}

impl BezierPath {
    /// Get a clone of the internal kurbo path
    fn to_kurbo_path(&self) -> BezPath {
        self.path.lock().unwrap().clone()
    }
}

fn convert_contours_to_paths(contours: linesweeper::topology::Contours) -> Vec<BezierPath> {
    contours
        .contours()
        .map(|contour| {
            BezierPath::from_kurbo_path(contour.path.clone())
        })
        .collect()
}


// Create a wrapper for the result since UniFFI doesn't support Vec<Object>
#[derive(uniffi::Record)]
pub struct BooleanOperationResult {
    pub paths: Vec<std::sync::Arc<BezierPath>>,
}

#[uniffi::export]
pub fn boolean_operation(
    path_a: &BezierPath,
    path_b: &BezierPath,
    operation: BooleanOperation,
    fill_rule: FillRule,
) -> Result<BooleanOperationResult, LineSweeperError> {
    // Convert our paths to kurbo paths
    let kurbo_a = path_a.to_kurbo_path();
    let kurbo_b = path_b.to_kurbo_path();

    let result = binary_op(
        &kurbo_a,
        &kurbo_b,
        LsFillRule::from(fill_rule),
        BinaryOp::from(operation),
    )?;

    // Convert results back to our BezierPath type
    let paths = convert_contours_to_paths(result)
        .into_iter()
        .map(std::sync::Arc::new)
        .collect();

    Ok(BooleanOperationResult { paths })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_basic_path_building() {
        let path = BezierPath::new();
        path.move_to(0.0, 0.0);
        path.line_to(1.0, 0.0);
        path.line_to(1.0, 1.0);
        path.line_to(0.0, 1.0);
        path.close_path();

        // Verify the kurbo path is not empty
        let kurbo_path = path.to_kurbo_path();
        assert!(!kurbo_path.is_empty());
    }

    #[test]
    fn test_curve_path() {
        let path = BezierPath::new();
        path.move_to(0.0, 0.0);
        path.curve_to(0.0, 1.0, 1.0, 1.0, 1.0, 0.0);
        path.close_path();

        let kurbo_path = path.to_kurbo_path();
        assert!(!kurbo_path.is_empty());
    }

    #[test]
    fn test_quad_path() {
        let path = BezierPath::new();
        path.move_to(0.0, 0.0);
        path.quad_to(0.5, 1.0, 1.0, 0.0);
        path.close_path();

        let kurbo_path = path.to_kurbo_path();
        assert!(!kurbo_path.is_empty());
    }

    #[test]
    fn test_basic_operation() {
        // Create a simple square using the new API
        let square = BezierPath::new();
        square.move_to(0.0, 0.0);
        square.line_to(1.0, 0.0);
        square.line_to(1.0, 1.0);
        square.line_to(0.0, 1.0);
        square.close_path();

        // Create another square offset by 0.5
        let offset_square = BezierPath::new();
        offset_square.move_to(0.5, 0.5);
        offset_square.line_to(1.5, 0.5);
        offset_square.line_to(1.5, 1.5);
        offset_square.line_to(0.5, 1.5);
        offset_square.close_path();

        // Test union operation
        let result = boolean_operation(
            &square,
            &offset_square,
            BooleanOperation::Union,
            FillRule::EvenOdd,
        );

        assert!(result.is_ok());
        let paths = result.unwrap();
        assert!(!paths.paths.is_empty());
    }

    #[test]
    fn test_segment_extraction() {
        let path = BezierPath::new();
        path.move_to(0.0, 0.0);
        path.line_to(1.0, 0.0);
        path.line_to(1.0, 1.0);
        path.close_path();

        let segments = path.get_segments();
        assert_eq!(segments.len(), 4);

        match &segments[0] {
            PathSegment::MoveTo { x, y } => {
                assert_eq!(*x, 0.0);
                assert_eq!(*y, 0.0);
            }
            _ => panic!("Expected MoveTo segment"),
        }

        match &segments[1] {
            PathSegment::LineTo { x, y } => {
                assert_eq!(*x, 1.0);
                assert_eq!(*y, 0.0);
            }
            _ => panic!("Expected LineTo segment"),
        }

        match &segments[2] {
            PathSegment::LineTo { x, y } => {
                assert_eq!(*x, 1.0);
                assert_eq!(*y, 1.0);
            }
            _ => panic!("Expected LineTo segment"),
        }

        match &segments[3] {
            PathSegment::ClosePath => {}
            _ => panic!("Expected ClosePath segment"),
        }
    }

    #[test]
    fn test_curve_segment_extraction() {
        let path = BezierPath::new();
        path.move_to(0.0, 0.0);
        path.curve_to(0.0, 1.0, 1.0, 1.0, 1.0, 0.0);
        path.close_path();

        let segments = path.get_segments();
        assert_eq!(segments.len(), 3);

        match &segments[1] {
            PathSegment::CurveTo { cp1_x, cp1_y, cp2_x, cp2_y, x, y } => {
                assert_eq!(*cp1_x, 0.0);
                assert_eq!(*cp1_y, 1.0);
                assert_eq!(*cp2_x, 1.0);
                assert_eq!(*cp2_y, 1.0);
                assert_eq!(*x, 1.0);
                assert_eq!(*y, 0.0);
            }
            _ => panic!("Expected CurveTo segment"),
        }
    }

    #[test]
    fn test_quad_segment_extraction() {
        let path = BezierPath::new();
        path.move_to(0.0, 0.0);
        path.quad_to(0.5, 1.0, 1.0, 0.0);
        path.close_path();

        let segments = path.get_segments();
        assert_eq!(segments.len(), 3);

        match &segments[1] {
            PathSegment::QuadTo { cp_x, cp_y, x, y } => {
                assert_eq!(*cp_x, 0.5);
                assert_eq!(*cp_y, 1.0);
                assert_eq!(*x, 1.0);
                assert_eq!(*y, 0.0);
            }
            _ => panic!("Expected QuadTo segment"),
        }
    }
}

// Generate the UniFFI scaffolding
uniffi::setup_scaffolding!();
